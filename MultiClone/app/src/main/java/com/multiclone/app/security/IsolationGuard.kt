package com.multiclone.app.security

import java.io.File

object IsolationGuard {
    private val safeIdRegex = Regex("^[a-zA-Z0-9._-]{1,64}$")

    fun validateInstanceId(id: String): Boolean = safeIdRegex.matches(id)

    fun resolveContainedDir(baseRoot: File, childName: String): File {
        require(validateInstanceId(childName)) { "Invalid instance identifier: $childName" }
        val canonicalBase = baseRoot.canonicalFile
        val resolved = File(canonicalBase, childName).canonicalFile
        require(resolved.path.startsWith(canonicalBase.path + File.separator)) {
            "Path traversal blocked outside sandbox root"
        }
        return resolved
    }
}
