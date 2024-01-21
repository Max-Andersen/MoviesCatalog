package com.example.moviecatalog.mainScreen.profileScreen

import UserDataResponse
import android.text.TextUtils
import android.util.Patterns
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import com.example.moviecatalog.network.Network
import com.example.moviecatalog.network.User.Gender
import com.example.moviecatalog.repository.AuthRepository
import com.example.moviecatalog.repository.UserRepository
import java.util.Calendar

class ProfileViewModel() : ViewModel() {
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()

    var email = ""
    var avatarLink = ""
    var name = ""
    var dateOfBirthday = ""
    var gender = Gender.MALE
    var nickname = ""

    fun changeGender(
        currentGender: MutableState<Gender>,
        buttonType: Gender
    ) {
        if (currentGender.value != buttonType) {
            currentGender.value = buttonType
        }
    }

    private fun normalizeDate(date: String): String {
        var result = ""
        result += date.slice(6..9) + "-"
        result += date.slice(3..4) + "-"
        result += date.slice(0..1)
        result += "T18:14:23.985Z"
        return result
    }

    private fun unNormalizeDate(date: String): String {
        var result = ""
        result += date.slice(8..9) + "."
        result += date.slice(5..6) + "."
        result += date.slice(0..3)
        return result
    }

    suspend fun getInformation() {
        userRepository.getData().collect {
            it.onSuccess { data ->
                email = data.email
                avatarLink = if (data.avatarLink == null) "" else data.avatarLink
                name = data.name
                dateOfBirthday = unNormalizeDate(data.birthDate)
                gender = Gender.fromServerIdToEnumValue(data.gender)
                nickname = data.nickName
            }

        }
    }

    suspend fun putInformation(
        enteredEmail: String,
        enteredAvatarLink: String = "",
        enteredName: String,
        enteredBirthDay: String,
        enteredGender: Gender
    ): String {
        val emailCorrect =
            !TextUtils.isEmpty(enteredEmail) && Patterns.EMAIL_ADDRESS.matcher(enteredEmail)
                .matches()
        var resultMessage = ""

        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)


        val enteredDay = enteredBirthDay.substringBefore('.').toInt()
        val enteredMonth = enteredBirthDay.substringAfter('.').substringBefore(".").toInt()
        val enteredYear = enteredBirthDay.substringAfterLast('.').toInt()

        if (!emailCorrect) {
            resultMessage += "\nНеверная почта!"
        }


        if ((enteredYear > currentYear) ||
            (enteredYear == currentYear && enteredMonth > currentMonth) ||
            (enteredYear == currentYear && enteredMonth == currentMonth && enteredDay >= currentDay)
        ) {
            resultMessage += "\nДата рождения должна быть меньше текущего дня!"
        }

        val normalizedDate = normalizeDate(enteredBirthDay)

        var answer = ""

        if (resultMessage.isEmpty()) {
            userRepository.putData(
                UserDataResponse(
                    id = Network.userId,
                    nickName = nickname,
                    email = enteredEmail,
                    avatarLink = enteredAvatarLink,
                    name = enteredName,
                    birthDate = normalizedDate,
                    gender = enteredGender.serverId
                )
            )
        } else {
            answer = resultMessage
        }
        return answer
    }

    fun logout() {
        authRepository.logout()
    }
}