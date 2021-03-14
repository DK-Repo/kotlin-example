package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email, password)
            .also {
                if (map.containsKey(it.login)) throw IllegalArgumentException("A user with this email already exists")
                map[it.login] = it
            }
    }

    fun loginUser(login: String, password: String): String? {
        val _login = login.toLoginView()
        return if (_login.matches("^[+][0-9]{11}$".toRegex())) {
            map[_login]?.run {
                if (checkPassword(password)) this.userInfo
                else null
            }
        } else {
            map[login.toLowerCase().trim()]
                ?.run {
                    if (checkPassword(password)) this.userInfo
                    else null
                }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        return User.makeUser(fullName, rawPhone = rawPhone)
            .also {
                if (map.containsKey(it.login)) throw IllegalArgumentException("A user with this phone already exists")
                map[it.login] = it
            }
    }

    fun requestAccessCode(phone: String) {
        map[phone.toLoginView()]?.let {
            val code = it.generateAccessCode()
            it.changePassword(it.accessCode!!, code)
            it.accessCode = code
            it.sendAccessCodeToUser(phone, code = it.accessCode!!)
        }
    }

    fun importUsers(list: List<String>): List<User> {
        val listOfUsers = mutableListOf<User>()
        list.forEach {
            val parts = it.split(";")

        }
        return listOfUsers
    }

    private fun String.toLoginView(): String {
        return this.replace("[^+\\d]".toRegex(), "")
    }
}
