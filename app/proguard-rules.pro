-keep class ir.mahdiparastesh.migratio.data.Country { <fields>; }
-keep class ir.mahdiparastesh.migratio.data.Criterion { <fields>; }
-keep class ir.mahdiparastesh.migratio.data.MyCriterion { <fields>; }

# Required by Android Gradle plugin
-dontwarn javax.annotation.Nullable

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken
