package utilities

import com.github.javafaker.Faker
import exceptions.InputValidationException
import graph_components.Property
import kotlin.random.Random

enum class FakeGenerator(val generatorName: String) {
    // Names
    FIRSTNAME("firstName"),
    LASTNAME("lastName"),
    FULLNAME("fullName"),


    // Address
    COUNTRY("country"),
    CITY("city"),
    STATE("state"),
    STREET_ADDRESS("streetAddress"),
    STREET_NAME("streetName"),
    ZIP_CODE("zipCode"),
    LATITUDE("latitude"),
    LONGITUDE("longitude"),


    // Business
    CREDIT_CARD_NUMBER("creditCardNumber"),
    CREDIT_CARD_TYPE("creditCardType"),


    // Company
    COMPANY_NAME("companyName"),


    // Internet
    AVATAR_URL("avatarUrl"),
    EMAIL_ADDRESS("email"),
    URL("url"),
    IPV4("ipv4"),


    // Lorem
    PARAGRAPH("paragraph"),
    SENTENCE("sentence"),
    WORD("word"),


    // Phone
    PHONE_NUMBER("phoneNumber"),


    // Time
    UNIX_TIME("unixTime"),


    // Numbers
    NUMBER_BETWEEN("numberBetween"),
    RANDOM_NUMBER("randomNumber"),

}

class ValueFaker(seedForRandom: Long = 42) {

    private val faker = Faker(java.util.Random(seedForRandom))
    private val random = Random(seedForRandom)

    fun getValue(property: Property): Any {
        val generator: FakeGenerator
        val requiredGeneratorName = property.generatorName
        try {
            generator = FakeGenerator.valueOf(requiredGeneratorName)
        } catch (exception: IllegalArgumentException) {
            throw InputValidationException("Could not find a faker for $requiredGeneratorName")
        }
        when (generator) {
            FakeGenerator.FIRSTNAME -> return faker.name().firstName()
            FakeGenerator.LASTNAME -> return faker.name().lastName()
            FakeGenerator.FULLNAME -> return faker.name().fullName()
            FakeGenerator.NUMBER_BETWEEN -> {
                val min = (property.parameters!![0] as String).toInt()
                val max = (property.parameters[1] as String).toInt()
                return faker.number().numberBetween(min, max)
            }
            FakeGenerator.COUNTRY -> return faker.address().country()
            FakeGenerator.CITY -> return faker.address().city()
            FakeGenerator.STATE -> return faker.address().state()
            FakeGenerator.STREET_ADDRESS -> return faker.address().streetAddress()
            FakeGenerator.STREET_NAME -> return faker.address().streetName()
            FakeGenerator.ZIP_CODE -> return faker.address().zipCode()
            FakeGenerator.LATITUDE -> return faker.address().latitude()
            FakeGenerator.LONGITUDE -> return faker.address().longitude()
            FakeGenerator.CREDIT_CARD_NUMBER -> return faker.business().creditCardNumber()
            FakeGenerator.CREDIT_CARD_TYPE -> return faker.business().creditCardType()
            FakeGenerator.COMPANY_NAME -> return faker.company().name()
            FakeGenerator.AVATAR_URL -> return faker.internet().avatar()
            FakeGenerator.EMAIL_ADDRESS -> return faker.internet().emailAddress()
            FakeGenerator.URL -> return faker.internet().emailAddress()
            FakeGenerator.IPV4 -> { // Some strange problem of methodNotFound with the faker implementation
                return "${random.nextInt(from = 0, until = 255)}".repeat(n = 4)
            }
            FakeGenerator.PARAGRAPH -> return faker.lorem().paragraph()
            FakeGenerator.SENTENCE -> return faker.lorem().sentence()
            FakeGenerator.WORD -> return faker.lorem().word()
            FakeGenerator.PHONE_NUMBER -> return faker.phoneNumber().cellPhone()
            FakeGenerator.UNIX_TIME -> return faker.date().birthday(0, 100)
            FakeGenerator.RANDOM_NUMBER -> return faker.number().randomNumber()
        }
    }

    fun getValues(property: Property, number: Int): List<Any> {
        val values = arrayListOf<Any>()
        for (i in 1..number)
            values.add(getValue(property = property))
        return values
    }

}
