# Gson: الحقول تُقرأ بالانعكاس — يجب أن تبقى أسماؤها كما هي
-keepclassmembers class com.oman.prayertimes.model.DayTimes { <fields>; }
-keepclassmembers class com.oman.prayertimes.data.PrayerRepository$YearCache { <fields>; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepattributes Signature
