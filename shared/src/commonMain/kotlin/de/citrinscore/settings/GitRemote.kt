package de.citrinscore.settings

import kotlinx.serialization.Serializable

@Serializable
data class GitRemote(val url: String, val token: String, val username: String = "") {
    override fun toString() = "GitRemote(url=$url, username=$username)"
}
