package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    private val email: String? = null,
    private val rawPhone: String? = null,
    private val meta: Map<String, Any>? = null
) {
    private val fullName: String
        get() = "${firstName.capitalize()} " +
                "${lastName?.capitalize()}"

    private val initials: String
        get() = "${firstName.first().toUpperCase()} " +
                "${lastName?.first()?.toUpperCase()}"

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
                .also {
                    if (!it?.matches("^[+][0-9]{11}$".toRegex())!!) throw throw IllegalArgumentException(
                        "Enter a valid phone number starting with a + and containing 11 digits"
                    )
                }
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    init {
        println("First init block, primary constructor was called")

        check(!firstName.isBlank()) { "FirstName must be not null" }
        check(email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email of phone must be not blank" }
    }

    val userInfo: String
        get() = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    private val salt: String by lazy {
        ByteArray(16)
            .also { SecureRandom().nextBytes(it) }
            .toString()
    }

    private lateinit var passwordHash: String

    // for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        login = email
        passwordHash = encrypt(password)
    }

    // for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        phone = rawPhone
        login = phone!!
        sendAccessCodeToUser(rawPhone, code)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    internal fun sendAccessCodeToUser(phone: String?, code: String) {
        println("..... sending access code: $code on $phone")
    }

    internal fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXUZabcdefghijklmnopqrstuvwxuz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random()
                    .also { append(possible[it]) }
            }
        }.toString()
    }

    private fun encrypt(password: String) = salt.plus(password.md5())

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {
        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            rawPhone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !rawPhone.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    rawPhone
                )
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password
                )
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.split(" ")
                .run {
                    when (size) {
                        1 -> if (first().isNotBlank()) first() to null else throw IllegalArgumentException()
                        2 -> first() to last()
                        else -> throw IllegalArgumentException(
                            "FullName must contain only first name " +
                                    "and last name, current split result $this"
                        )
                    }
                }
        }
    }
}


