package com.seapip.thomas.wearify.wearify

import java.util.*

object Model {
    data class Token(
            var token: String? = null,
            var key: String? = null,
            var error: String? = null,
            var accessToken: String? = null,
            var tokenType: String? = null,
            var expiresIn: Int? = null,
            var refreshToken: String? = null,
            var scope: String? = null
    )
}