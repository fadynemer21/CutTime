package com.fadynemer.cutime.data

import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.model.OpeningHours

object SampleBarberData {

    val barberShops = listOf(
        BarberShop(
            id = "barber_1",
            name = "Urban Fade Studio",
            rating = 4.9,
            reviewCount = 128,
            startingPrice = 60,
            nextAvailable = "Today at 14:30",
            description = "Modern cuts, clean fades, and careful beard styling in a relaxed studio.",
            services = listOf(
                BarberService("service_1", "Classic Haircut", 60, 30),
                BarberService("service_2", "Skin Fade", 75, 45),
                BarberService("service_3", "Haircut and Beard", 95, 60)
            ),
            openingHours = standardOpeningHours(),
            galleryItemCount = 4,
            availableTimes = listOf("14:30", "16:00", "17:30")
        ),
        BarberShop(
            id = "barber_2",
            name = "Classic Cuts",
            rating = 4.8,
            reviewCount = 94,
            startingPrice = 50,
            nextAvailable = "Today at 16:00",
            description = "Traditional barbering with friendly service for adults and children.",
            services = listOf(
                BarberService("service_1", "Classic Haircut", 50, 30),
                BarberService("service_2", "Children's Haircut", 45, 30),
                BarberService("service_3", "Beard Trim", 35, 20)
            ),
            openingHours = standardOpeningHours(),
            galleryItemCount = 3,
            availableTimes = listOf("16:00", "17:00", "18:30")
        ),
        BarberShop(
            id = "barber_3",
            name = "Sharp Style Barbers",
            rating = 4.7,
            reviewCount = 76,
            startingPrice = 55,
            nextAvailable = "Tomorrow at 10:30",
            description = "Detailed cuts and contemporary styling tailored to your look.",
            services = listOf(
                BarberService("service_1", "Haircut", 55, 30),
                BarberService("service_2", "Fade and Styling", 70, 45),
                BarberService("service_3", "Full Grooming", 110, 75)
            ),
            openingHours = standardOpeningHours(),
            galleryItemCount = 4,
            availableTimes = listOf("10:30", "12:00", "15:30")
        ),
        BarberShop(
            id = "barber_4",
            name = "The Barber Room",
            rating = 4.6,
            reviewCount = 51,
            startingPrice = 45,
            nextAvailable = "Tomorrow at 12:00",
            description = "A comfortable neighborhood barbershop for dependable cuts and grooming.",
            services = listOf(
                BarberService("service_1", "Haircut", 45, 30),
                BarberService("service_2", "Beard Shape", 30, 20),
                BarberService("service_3", "Haircut and Beard", 70, 50)
            ),
            openingHours = standardOpeningHours(),
            galleryItemCount = 3,
            availableTimes = listOf("12:00", "13:30", "16:30")
        )
    )

    fun findById(barberId: String): BarberShop? {
        return barberShops.find { barberShop ->
            barberShop.id == barberId
        }
    }

    private fun standardOpeningHours() = listOf(
        OpeningHours("Sunday - Thursday", "09:00 - 19:00"),
        OpeningHours("Friday", "09:00 - 14:00"),
        OpeningHours("Saturday", "Closed")
    )
}
