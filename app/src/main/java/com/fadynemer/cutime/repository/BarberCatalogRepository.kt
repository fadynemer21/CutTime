package com.fadynemer.cutime.repository

import com.fadynemer.cutime.BuildConfig
import com.fadynemer.cutime.data.BarberCatalogCache
import com.fadynemer.cutime.data.SampleBarberData
import com.fadynemer.cutime.model.BarberCatalog
import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberAvailabilityDocumentCodec
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.model.BarberShopReadinessEvaluator
import com.fadynemer.cutime.model.CatalogSource
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.model.OpeningHours
import com.fadynemer.cutime.model.effectiveWorkingPeriods
import com.fadynemer.cutime.util.NextAvailabilityFormatter
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger

interface BarberCatalogDataSource {
    fun observeCatalog(
        onResult: (Result<BarberCatalog>) -> Unit
    ): AppointmentObservation

    fun loadBarber(
        barberId: String,
        onResult: (Result<BarberShop?>) -> Unit
    )

    fun observeOccupiedTimes(
        barberId: String,
        date: String,
        onResult: (Result<Set<String>>) -> Unit
    ): AppointmentObservation
}

class BarberCatalogRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),
    private val includeDevelopmentFallback: Boolean =
        BuildConfig.ENABLE_DEVELOPMENT_CATALOG
) : BarberCatalogDataSource {

    override fun observeCatalog(
        onResult: (Result<BarberCatalog>) -> Unit
    ): AppointmentObservation {
        val registration =
            firestore
                .collection(PROFILES_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        deliverFailureOrFallback(
                            error = error,
                            onResult = onResult
                        )
                        return@addSnapshotListener
                    }

                    val profiles = snapshot?.documents.orEmpty()

                    if (profiles.isEmpty()) {
                        deliverEmptyOrFallback(onResult)
                    } else {
                        aggregateProfiles(profiles) { result ->
                            result
                                .onSuccess { barbers ->
                                    if (barbers.isEmpty()) {
                                        deliverEmptyOrFallback(onResult)
                                    } else {
                                        BarberCatalogCache.replace(barbers)
                                        onResult(
                                            Result.success(
                                                BarberCatalog(
                                                    barbers = barbers,
                                                    source =
                                                        CatalogSource.FIRESTORE
                                                )
                                            )
                                        )
                                    }
                                }
                                .onFailure { aggregationError ->
                                    deliverFailureOrFallback(
                                        error = aggregationError,
                                        onResult = onResult
                                    )
                                }
                        }
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun loadBarber(
        barberId: String,
        onResult: (Result<BarberShop?>) -> Unit
    ) {
        BarberCatalogCache.find(
            barberId = barberId,
            includeDevelopmentFallback = includeDevelopmentFallback
        )?.let { cached ->
            onResult(Result.success(cached))
            return
        }

        firestore
            .collection(PROFILES_COLLECTION)
            .document(barberId)
            .get()
            .addOnSuccessListener { profile ->
                if (!profile.exists()) {
                    onResult(Result.success(null))
                } else {
                    aggregateProfile(profile, onResult)
                }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun observeOccupiedTimes(
        barberId: String,
        date: String,
        onResult: (Result<Set<String>>) -> Unit
    ): AppointmentObservation {
        val registration =
            firestore
                .collection(SLOTS_COLLECTION)
                .whereEqualTo("barberId", barberId)
                .whereEqualTo("appointmentDate", date)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        val times =
                            snapshot
                                ?.documents
                                ?.mapNotNull { document ->
                                    document.id
                                        .substringAfterLast("_")
                                        .replace("-", ":")
                                        .takeIf { value ->
                                            value.matches(
                                                Regex("\\d{2}:\\d{2}")
                                            )
                                        }
                                }
                                ?.toSet()
                                .orEmpty()

                        onResult(Result.success(times))
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    private fun aggregateProfiles(
        profiles: List<DocumentSnapshot>,
        onResult: (Result<List<BarberShop>>) -> Unit
    ) {
        val remaining = AtomicInteger(profiles.size)
        val barbers = mutableListOf<BarberShop>()
        val errors = mutableListOf<Throwable>()

        profiles.forEach { profile ->
            aggregateProfile(profile) { result ->
                synchronized(barbers) {
                    result
                        .onSuccess { barber ->
                            if (barber != null) {
                                barbers += barber
                            }
                        }
                        .onFailure(errors::add)

                    if (remaining.decrementAndGet() == 0) {
                        if (barbers.isNotEmpty()) {
                            onResult(
                                Result.success(
                                    barbers.sortedBy { it.name }
                                )
                            )
                        } else {
                            onResult(
                                Result.failure(
                                    errors.firstOrNull()
                                        ?: Exception(
                                            "No complete barber profiles."
                                        )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun aggregateProfile(
        profile: DocumentSnapshot,
        onResult: (Result<BarberShop?>) -> Unit
    ) {
        val barberId = profile.id
        var services: List<BarberService>? = null
        var availability: BarberAvailability? = null
        var occupiedTimesByDate: Map<String, Set<String>>? = null
        var firstError: Throwable? = null
        val remaining = AtomicInteger(3)

        fun finish() {
            if (remaining.decrementAndGet() != 0) return

            val serviceList = services.orEmpty()
            val barberAvailability = availability

            val aggregationError = firstError
            if (aggregationError != null) {
                onResult(Result.failure(aggregationError))
                return
            }

            val name = profile.getString("shopName").orEmpty()
            val description =
                profile.getString("description").orEmpty()

            if (
                !BarberShopReadinessEvaluator.profileComplete(
                    name,
                    description
                ) ||
                serviceList.isEmpty() ||
                barberAvailability == null ||
                barberAvailability.days.size != 7 ||
                barberAvailability.days.none { it.isOpen }
            ) {
                onResult(Result.success(null))
                return
            }
            val days = barberAvailability.days

            val rating =
                profile.getDouble("ratingAverage") ?: 0.0
            val ratingCount =
                profile.getLong("ratingCount")?.toInt() ?: 0
            val openingHours =
                days.map { day ->
                    OpeningHours(
                        day = day.day,
                        hours =
                            if (day.isOpen) {
                                day.effectiveWorkingPeriods()
                                    .joinToString(", ") { period ->
                                        "${period.startTime} - ${period.endTime}"
                                    }
                            } else {
                                "Closed"
                            }
                    )
                }
            val nextAvailable =
                NextAvailabilityFormatter.format(
                    availability = barberAvailability,
                    durationMinutes =
                        serviceList.minOf { it.durationMinutes },
                    occupiedTimesByDate =
                        occupiedTimesByDate.orEmpty()
                )

            val barber = BarberShop(
                id = barberId,
                name = name,
                rating = rating,
                reviewCount = ratingCount,
                startingPrice =
                    serviceList.minOfOrNull { it.price } ?: 0,
                nextAvailable = nextAvailable,
                description = description,
                services = serviceList,
                openingHours = openingHours,
                galleryItemCount = 0,
                availableTimes =
                    generateRepresentativeTimes(days),
                availability = barberAvailability
            )

            BarberCatalogCache.replace(
                (
                    BarberCatalogCache.all()
                        .filterNot { it.id == barber.id } +
                        barber
                    )
            )
            onResult(Result.success(barber))
        }

        firestore
            .collection(PROFILES_COLLECTION)
            .document(barberId)
            .collection(SERVICES_COLLECTION)
            .get()
            .addOnSuccessListener { snapshot ->
                services =
                    snapshot.documents.mapNotNull(::mapService)
                finish()
            }
            .addOnFailureListener { error ->
                firstError = error
                services = emptyList()
                finish()
            }

        firestore
            .collection(AVAILABILITY_COLLECTION)
            .document(barberId)
            .get()
            .addOnSuccessListener { snapshot ->
                availability =
                    if (snapshot.exists()) {
                        mapAvailability(snapshot)
                    } else {
                        null
                    }
                finish()
            }
            .addOnFailureListener { error ->
                firstError = firstError ?: error
                finish()
            }

        val today = LocalDate.now()
        firestore
            .collection(SLOTS_COLLECTION)
            .whereEqualTo("barberId", barberId)
            .whereGreaterThanOrEqualTo(
                "appointmentDate",
                today.toString()
            )
            .whereLessThanOrEqualTo(
                "appointmentDate",
                today.plusDays(13).toString()
            )
            .get()
            .addOnSuccessListener { snapshot ->
                occupiedTimesByDate =
                    snapshot.documents
                        .groupBy { document ->
                            document.getString("appointmentDate").orEmpty()
                        }
                        .mapValues { (_, documents) ->
                            documents.mapNotNull { document ->
                                document.id
                                    .substringAfterLast("_")
                                    .replace("-", ":")
                                    .takeIf { it.matches(Regex("\\d{2}:\\d{2}")) }
                            }.toSet()
                        }
                finish()
            }
            .addOnFailureListener { error ->
                firstError = firstError ?: error
                finish()
            }
    }

    private fun mapService(
        document: DocumentSnapshot
    ): BarberService? {
        val service = BarberService(
            id =
                document.getString("serviceId")
                    ?: document.id,
            name =
                document.getString("name")
                    ?: return null,
            price =
                document.getLong("price")?.toInt()
                    ?: return null,
            durationMinutes =
                document.getLong("durationMinutes")?.toInt()
                    ?: return null
        )
        return service.takeIf {
            it.name.trim().length >= 2 &&
                it.price > 0 &&
                it.durationMinutes > 0 &&
                it.durationMinutes % 15 == 0
        }
    }

    private fun mapAvailability(
        document: DocumentSnapshot
    ): BarberAvailability? {
        val rawDays =
            document.get("days") as? List<*>
        val dayMaps =
            rawDays
                ?.mapNotNull { raw ->
                    @Suppress("UNCHECKED_CAST")
                    raw as? Map<String, Any?>
                }
                .orEmpty()
        val (saved, _) =
            BarberShopReadinessEvaluator.availabilityState(
                exists = document.exists(),
                rawDays = dayMaps
            )
        if (!saved) return null

        return BarberAvailabilityDocumentCodec.decode(
            rawDays = rawDays,
            rawBlockedDates = document.get("blockedDates")
        )
    }

    private fun generateRepresentativeTimes(
        days: List<DayAvailability>
    ): List<String> {
        val openDay = days.firstOrNull { it.isOpen }
            ?: return emptyList()
        val result = mutableListOf<String>()
        openDay.effectiveWorkingPeriods().forEach { period ->
            val start = runCatching {
                LocalTime.parse(period.startTime)
            }.getOrNull() ?: return@forEach
            val end = runCatching {
                LocalTime.parse(period.endTime)
            }.getOrNull() ?: return@forEach
            var cursor = start
            while (cursor.isBefore(end) && result.size < 12) {
                result += cursor.toString()
                cursor = cursor.plusMinutes(30)
            }
        }

        return result.distinct().sorted()
    }

    private fun deliverFallback(
        onResult: (Result<BarberCatalog>) -> Unit
    ) {
        val fallback =
            SampleBarberData.barberShops.map { barber ->
                barber.copy(isDevelopmentFallback = true)
            }
        BarberCatalogCache.replace(fallback)
        onResult(
            Result.success(
                BarberCatalog(
                    barbers = fallback,
                    source = CatalogSource.DEVELOPMENT_FALLBACK
                )
            )
        )
    }

    private fun deliverEmptyOrFallback(
        onResult: (Result<BarberCatalog>) -> Unit
    ) {
        if (includeDevelopmentFallback) {
            deliverFallback(onResult)
            return
        }
        BarberCatalogCache.replace(emptyList())
        onResult(
            Result.success(
                BarberCatalog(
                    barbers = emptyList(),
                    source = CatalogSource.FIRESTORE
                )
            )
        )
    }

    private fun deliverFailureOrFallback(
        error: Throwable,
        onResult: (Result<BarberCatalog>) -> Unit
    ) {
        if (includeDevelopmentFallback) {
            deliverFallback(onResult)
        } else {
            onResult(Result.failure(error))
        }
    }

    private companion object {
        const val PROFILES_COLLECTION = "barberProfiles"
        const val SERVICES_COLLECTION = "services"
        const val AVAILABILITY_COLLECTION = "barberAvailability"
        const val SLOTS_COLLECTION = "bookingSlots"
    }
}
