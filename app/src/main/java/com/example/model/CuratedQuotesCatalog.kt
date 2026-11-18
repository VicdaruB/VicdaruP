package com.example.model

data class CuratedQuote(
    val anime: AnimeSeries,
    val characterName: String,
    val text: String,
    val moodTag: String,
)

object CuratedQuotesCatalog {
    val quotes: List<CuratedQuote> = listOf(
        // Los Diarios de la Boticaria
        CuratedQuote(
            AnimeSeries.APOTHECARY,
            "Maomao",
            "Incluso el veneno más amargo tiene cura cuando observas los detalles con calma y paciencia.",
            "Calma"
        ),
        CuratedQuote(
            AnimeSeries.APOTHECARY,
            "Maomao",
            "No tienes que resolver toda tu vida hoy; basta con cuidar tu cuerpo, tomar agua tibia y dar el siguiente paso.",
            "Autocuidado"
        ),
        CuratedQuote(
            AnimeSeries.APOTHECARY,
            "Jinshi",
            "Tu mente es más valiosa de lo que crees en los días grises. Permítete descansar sin culpa.",
            "Esperanza"
        ),
        CuratedQuote(
            AnimeSeries.APOTHECARY,
            "Maomao",
            "La curiosidad y un pequeño gesto amable contigo mismo pueden cambiar por completo el rumbo de una tarde pesada.",
            "Concentración"
        ),
        CuratedQuote(
            AnimeSeries.APOTHECARY,
            "Maomao",
            "Cada hierba necesita su propio tiempo para florecer; no te compares con el reloj de nadie más.",
            "Serenidad"
        ),
        CuratedQuote(
            AnimeSeries.APOTHECARY,
            "Jinshi",
            "Aunque el palacio parezca inmenso y ruidoso, siempre hay un rincón tranquilo donde tu voz importa.",
            "Autocuidado"
        ),

        // Dandadan
        CuratedQuote(
            AnimeSeries.DANDADAN,
            "Momo Ayase",
            "No dejes que el miedo te quite la chispa: incluso en medio del caos, tú tienes el poder de proteger tu alegría.",
            "Valentía"
        ),
        CuratedQuote(
            AnimeSeries.DANDADAN,
            "Okarun",
            "Avanzar aunque te tiemblen las piernas también es ser valiente. Hoy ya diste más de lo que imaginabas.",
            "Valentía"
        ),
        CuratedQuote(
            AnimeSeries.DANDADAN,
            "Momo Ayase",
            "Cuando sientas que el mundo va demasiado rápido, respira hondo y recuerda quién camina a tu lado.",
            "Esperanza"
        ),
        CuratedQuote(
            AnimeSeries.DANDADAN,
            "Okarun",
            "No necesitas ser perfecto para brillar; basta con no rendirte contigo mismo en los días difíciles.",
            "Alegría"
        ),
        CuratedQuote(
            AnimeSeries.DANDADAN,
            "Seiko Ayase",
            "Come algo rico, ordena tu espacio y sacúdete las malas vibras: mañana será otro combate que vas a ganar.",
            "Autocuidado"
        ),
        CuratedQuote(
            AnimeSeries.DANDADAN,
            "Momo Ayase",
            "Tu energía sincera atrae a las personas correctas. Sigue siendo auténtico, incluso cuando dudes.",
            "Alegría"
        ),

        // Little Witch Academia
        CuratedQuote(
            AnimeSeries.LITTLE_WITCH,
            "Akko Kagari",
            "Un corazón que cree en sí mismo es la verdadera magia. Cada intento fallido te acerca al hechizo correcto.",
            "Esperanza"
        ),
        CuratedQuote(
            AnimeSeries.LITTLE_WITCH,
            "Profesora Ursula",
            "No olvides que la magia nace de las palabras que te dices con cariño cuando nadie está mirando.",
            "Autocuidado"
        ),
        CuratedQuote(
            AnimeSeries.LITTLE_WITCH,
            "Akko Kagari",
            "Si hoy te caíste de la escoba, ríete un poco, limpia el polvo y vuelve a despegar a tu propio ritmo.",
            "Alegría"
        ),
        CuratedQuote(
            AnimeSeries.LITTLE_WITCH,
            "Lotte Yansson",
            "Tu sensibilidad no es debilidad; es la capacidad de notar la belleza que otros pasan por alto.",
            "Calma"
        ),
        CuratedQuote(
            AnimeSeries.LITTLE_WITCH,
            "Diana Cavendish",
            "La disciplina amable construye milagros silenciosos. Un hábito pequeño hoy vale oro mañana.",
            "Concentración"
        ),
        CuratedQuote(
            AnimeSeries.LITTLE_WITCH,
            "Akko Kagari",
            "El cielo no tiene límites cuando decides iluminar el día con una sonrisa genuina.",
            "Alegría"
        ),

        // Sousou no Frieren
        CuratedQuote(
            AnimeSeries.FRIEREN,
            "Frieren",
            "Hasta los momentos que parecen pequeños hoy se convertirán en recuerdos cálidos que iluminarán tu futuro.",
            "Serenidad"
        ),
        CuratedQuote(
            AnimeSeries.FRIEREN,
            "Himmel",
            "Si das un paso para ayudar a alguien o cuidarte a ti mismo hoy, ese día ya valió la pena por completo.",
            "Esperanza"
        ),
        CuratedQuote(
            AnimeSeries.FRIEREN,
            "Fern",
            "Mantener la calma en lo cotidiano también es una proeza. Celebra tus avances silenciosos.",
            "Calma"
        ),
        CuratedQuote(
            AnimeSeries.FRIEREN,
            "Frieren",
            "El viaje es largo precisamente para que tengas tiempo de disfrutar el paisaje, no solo la meta.",
            "Serenidad"
        ),
        CuratedQuote(
            AnimeSeries.FRIEREN,
            "Stark",
            "Está bien sentir miedo antes de empezar; el coraje aparece justo después del primer paso.",
            "Valentía"
        ),
        CuratedQuote(
            AnimeSeries.FRIEREN,
            "Himmel",
            "Recuerda sonreírle a tu propio esfuerzo: quien eres hoy está construyendo un camino admirable.",
            "Autocuidado"
        ),

        // Sakura Card Captor
        CuratedQuote(
            AnimeSeries.SAKURA,
            "Sakura Kinomoto",
            "¡Todo estará bien! Hoy es un nuevo capítulo y tienes todo lo necesario para llenarlo de luz.",
            "Esperanza"
        ),
        CuratedQuote(
            AnimeSeries.SAKURA,
            "Sakura Kinomoto",
            "Cuando la tristeza toque tu puerta, abre la ventana, deja entrar el aire fresco y recuerda que no estás solo.",
            "Autocuidado"
        ),
        CuratedQuote(
            AnimeSeries.SAKURA,
            "Tomoyo Daidouji",
            "Tu esfuerzo merece ser celebrado hoy, sin importar lo pequeño que parezca desde afuera.",
            "Alegría"
        ),
        CuratedQuote(
            AnimeSeries.SAKURA,
            "Syaoran Li",
            "La constancia tranquila supera cualquier tormenta. Sigue adelante con firmeza y corazón sereno.",
            "Concentración"
        ),
        CuratedQuote(
            AnimeSeries.SAKURA,
            "Kero",
            "A veces el mejor hechizo contra el desánimo es una pausa dulce, buena música y empezar de nuevo.",
            "Calma"
        ),
        CuratedQuote(
            AnimeSeries.SAKURA,
            "Sakura Kinomoto",
            "Libera las preocupaciones de ayer como pétalos al viento; hoy brillas con tu propia estrella.",
            "Esperanza"
        ),

        // Ranma 1/2
        CuratedQuote(
            AnimeSeries.RANMA,
            "Ranma Saotome",
            "Si la vida te lanza agua fría de golpe, tómalo con humor, adáptate y sigue peleando por lo que quieres.",
            "Valentía"
        ),
        CuratedQuote(
            AnimeSeries.RANMA,
            "Akane Tendo",
            "No ocultes lo que sientes ni te exijas ser invencible; pedir apoyo también te hace fuerte.",
            "Autocuidado"
        ),
        CuratedQuote(
            AnimeSeries.RANMA,
            "Ranma Saotome",
            "Cada entrenamiento difícil de hoy se transforma en agilidad y confianza para los retos de mañana.",
            "Concentración"
        ),
        CuratedQuote(
            AnimeSeries.RANMA,
            "Shampoo",
            "Ponle ritmo y entusiasmo a lo que haces hoy; cuando disfrutas el proceso, el cansancio pesa la mitad.",
            "Alegría"
        ),
        CuratedQuote(
            AnimeSeries.RANMA,
            "Ryoga Hibiki",
            "Incluso si sientes que te perdiste un momento en el camino, siempre puedes reorientar tus pasos y seguir.",
            "Esperanza"
        ),
        CuratedQuote(
            AnimeSeries.RANMA,
            "Akane Tendo",
            "Respira profundo antes de responderle al día: tu paz mental vale más que cualquier prisa.",
            "Calma"
        )
    )

    fun randomQuote(excludeText: String? = null): CuratedQuote {
        val pool = quotes.filter { it.text != excludeText }
        return (pool.ifEmpty { quotes }).random()
    }

    fun morningQuote(): CuratedQuote {
        val morningPool = quotes.filter { it.moodTag in listOf("Esperanza", "Alegría", "Serenidad") }
        return (morningPool.ifEmpty { quotes }).random()
    }

    val allMoodTags: List<String> = listOf("Esperanza", "Calma", "Serenidad", "Valentía", "Concentración", "Autocuidado", "Alegría")
}
