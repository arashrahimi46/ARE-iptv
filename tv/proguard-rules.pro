# R8 keep rules for the release build.
#
# The libraries we ship (Compose, Coil, Room, Media3, OkHttp, DataStore) all bundle their own
# consumer R8 rules, so the defaults handle them. These app-level keeps guard the bits R8 can't
# see are used reflectively / by name.

# Room entities & DAOs are referenced by generated code and by the DB schema; keep their members
# so column/field mapping isn't broken by renaming.
-keep class com.arashrahimi46.iptv.data.model.** { *; }
-keep class com.arashrahimi46.iptv.data.db.** { *; }

# Kotlin metadata for reflective coroutine/serialization internals.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
