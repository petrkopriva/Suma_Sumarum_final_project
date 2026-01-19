// Top-level build file
plugins {
    // Místo aliasů použijeme přímá ID a verze. Tím se vyhneme chybě "Unresolved reference".
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
}