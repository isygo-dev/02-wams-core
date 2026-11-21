package eu.isygoit.service.impl.planning;

import java.util.Map;
import java.util.Objects;

/**
 * Generic corrective guidance for request validation issues. Domain-specific remedies belong to clients.
 */
public final class ValidationRemediation {

    private ValidationRemediation() {
    }

    public static String forIssue(String language, String code, String path, Map<String, Object> args) {
        String lang = normalizeLanguage(language);
        String field = path == null || path.isBlank() ? translated(lang, "the request", "la requête", "الطلب")
                : translated(lang, "'" + path + "'", "le champ '" + path + "'", "الحقل '" + path + "'");
        Map<String, Object> values = args == null ? Map.of() : args;
        return switch (Objects.toString(code, "")) {
            case "MISSING_REQUIRED_FIELD" -> translated(lang,
                    "Provide the required value at " + field + ".",
                    "Renseignez la valeur obligatoire dans " + field + ".",
                    "أدخل القيمة المطلوبة في " + field + ".");
            case "LIMIT_EXCEEDED" -> {
                String limit = Objects.toString(values.get("limit"), "request");
                String maximum = Objects.toString(values.get("max"), "the allowed maximum");
                yield translated(lang,
                        "Reduce " + field + " below the configured " + limit + " limit of " + maximum
                                + ", or raise that server limit after reviewing capacity.",
                        "Réduisez " + field + " sous la limite configurée « " + limit + " » (" + maximum
                                + "), ou augmentez cette limite côté serveur après vérification de la capacité.",
                        "قلّل " + field + " إلى ما دون الحد المهيأ «" + limit + "» (" + maximum
                                + ")، أو ارفع حد الخادم بعد مراجعة السعة.");
            }
            case "UNKNOWN_REFERENCE" -> translated(lang,
                    "Use an identifier that exists in this request, or add the referenced task, resource, slot, calendar, setup matrix, or type definition.",
                    "Utilisez un identifiant présent dans la requête ou ajoutez l'objet référencé : tâche, ressource, créneau, calendrier, matrice de réglage ou définition de type.",
                    "استخدم معرّفاً موجوداً في الطلب، أو أضف العنصر المشار إليه: مهمة أو مورداً أو فترة أو تقويماً أو مصفوفة إعداد أو تعريف نوع.");
            case "DUPLICATE_ID" -> translated(lang,
                    "Assign a unique identifier to this item; IDs must not be reused within the same collection.",
                    "Attribuez un identifiant unique à cet élément ; un identifiant ne doit pas être réutilisé dans la même collection.",
                    "عيّن معرّفاً فريداً لهذا العنصر؛ يجب عدم تكرار المعرّف ضمن المجموعة نفسها.");
            case "TIME_NOT_ALIGNED" -> translated(lang,
                    "Move this time point to a horizon tick boundary; all task, slot, and assignment times must align with horizon.tickSeconds.",
                    "Alignez cette heure sur une limite de tick de l'horizon ; les heures des tâches, créneaux et affectations doivent respecter horizon.tickSeconds.",
                    "اضبط هذا الوقت على حدّ من حدود وحدات الأفق؛ يجب أن تتوافق أوقات المهام والفترات والتعيينات مع horizon.tickSeconds.");
            case "TIME_OUT_OF_HORIZON" -> translated(lang,
                    "Move this time point inside the configured horizon, including its start and end boundaries as allowed by the field.",
                    "Placez ce point temporel dans l'horizon configuré, en respectant les limites de début et de fin autorisées pour ce champ.",
                    "انقل هذا الوقت إلى داخل الأفق المحدد، مع احترام حدود البداية والنهاية المسموح بها لهذا الحقل.");
            case "INVALID_TIMEZONE" -> translated(lang,
                    "Set meta.timezone to a valid IANA time-zone identifier.",
                    "Renseignez meta.timezone avec un identifiant de fuseau horaire IANA valide.",
                    "عيّن meta.timezone إلى معرّف منطقة زمنية صالح وفق IANA.");
            case "INVALID_HORIZON", "HORIZON_TOO_LARGE" -> translated(lang,
                    "Set a positive horizon whose duration is an exact multiple of tickSeconds and keep its tick count within the configured limit.",
                    "Définissez un horizon positif dont la durée est un multiple exact de tickSeconds et dont le nombre de ticks respecte la limite configurée.",
                    "حدّد أفقاً موجباً تكون مدته مضاعفاً صحيحاً لـ tickSeconds، واجعل عدد وحداته ضمن الحد المهيأ.");
            case "CALENDAR_INVALID" -> translated(lang,
                    "Correct the calendar intervals at " + field + ": provide valid local start/end times, days, and date bounds.",
                    "Corrigez les intervalles du calendrier dans " + field + " : heures locales, jours et bornes de dates doivent être valides.",
                    "صحّح فترات التقويم في " + field + ": يجب أن تكون أوقات البداية والنهاية المحلية والأيام ونطاقات التواريخ صالحة.");
            case "INVALID_DURATION", "DURATION_REQUIRED" -> translated(lang,
                    "Set a positive duration in horizon ticks that fits the horizon, or provide valid start slots when using slot mode.",
                    "Définissez une durée positive en ticks qui tient dans l'horizon, ou fournissez des créneaux de début valides en mode créneaux.",
                    "حدّد مدة موجبة بوحدات الأفق وتناسبه، أو وفّر فترات بداية صالحة عند استخدام نمط الفترات.");
            case "INVALID_CAPACITY", "INVALID_DEMAND" -> translated(lang,
                    "Set capacity and demand to positive integers, and ensure each task's demand does not exceed the resource capacity.",
                    "Définissez la capacité et la demande avec des entiers positifs ; la demande d'une tâche ne doit pas dépasser la capacité de la ressource.",
                    "اجعل السعة والطلب عددين صحيحين موجبين، وتأكد من ألا يتجاوز طلب المهمة سعة المورد.");
            case "UNKNOWN_SOLVER" -> translated(lang,
                    "Choose a solver listed by the Plan service capabilities endpoint.",
                    "Choisissez un solveur répertorié par le point de terminaison des capacités du service Plan.",
                    "اختر محلّلاً مدرجاً في نقطة نهاية إمكانات خدمة Plan.");
            case "ENGINE_UNSUPPORTED_FEATURE", "ENGINE_UNSUPPORTED_CONSTRAINT",
                    "ENGINE_UNSUPPORTED_ENFORCEMENT", "ENGINE_UNSUPPORTED_OBJECTIVE" ->
                    translated(lang,
                            "Choose a solver that supports this feature or remove/change the unsupported request element; check the Plan service capabilities for the selected solver.",
                            "Choisissez un solveur compatible ou supprimez/modifiez l'élément non pris en charge ; consultez les capacités du service Plan pour le solveur choisi.",
                            "اختر محلّلاً يدعم هذه الميزة أو احذف/عدّل عنصر الطلب غير المدعوم؛ راجع إمكانات خدمة Plan للمحلّل المحدد.");
            case "PRECEDENCE_CYCLE" -> translated(lang,
                    "Remove or reverse one or more precedence links so the task dependency graph is acyclic.",
                    "Supprimez ou inversez un ou plusieurs liens de précédence afin que le graphe des dépendances soit sans cycle.",
                    "احذف أو اعكس رابط أسبقية واحداً أو أكثر لجعل مخطط اعتماد المهام خالياً من الدورات.");
            case "INVALID_SCOPE" -> translated(lang,
                    "Use a scope kind and value supported by the selected constraint, and make sure referenced IDs exist.",
                    "Utilisez un type et une valeur de portée compatibles avec la contrainte, et vérifiez l'existence des identifiants référencés.",
                    "استخدم نوع ونطاقاً تدعمهما القاعدة المحددة، وتأكد من وجود المعرّفات المشار إليها.");
            case "INVALID_REQUIREMENT" -> translated(lang,
                    "Correct the resource requirement: use a supported role, positive count and amount, and valid resource selectors.",
                    "Corrigez l'exigence de ressource : rôle pris en charge, nombre et quantité positifs, sélecteurs valides.",
                    "صحّح متطلب المورد باستخدام دور مدعوم وعدد وكمية موجبين ومحددات موارد صالحة.");
            case "INVALID_INITIAL_SOLUTION" -> translated(lang,
                    "Remove the invalid or duplicate initial assignment, or update its task, resource, and start references to match the request.",
                    "Supprimez l'affectation initiale invalide ou dupliquée, ou corrigez ses références de tâche, ressource et début.",
                    "احذف التعيين الابتدائي غير الصالح أو المكرر، أو صحّح مراجع المهمة والمورد ووقت البداية لتطابق الطلب.");
            case "UNSUPPORTED_SCHEMA_VERSION" -> translated(lang,
                    "Set meta.version to the schema version supported by this Plan service.",
                    "Renseignez meta.version avec la version de schéma prise en charge par ce service Plan.",
                    "عيّن meta.version إلى إصدار المخطط الذي تدعمه خدمة Plan.");
            default -> translated(lang,
                    "Correct the value at " + field + " using the stated constraint; check its references, units, and bounds in the error detail.",
                    "Corrigez la valeur dans " + field + " selon la contrainte indiquée ; vérifiez ses références, unités et limites dans le détail.",
                    "صحّح القيمة في " + field + " وفق القيد المذكور، وراجع المراجع والوحدات والحدود في تفاصيل الخطأ.");
        };
    }

    private static String normalizeLanguage(String language) {
        String value = language == null ? "" : language.trim().toLowerCase(java.util.Locale.ROOT);
        return value.startsWith("fr") ? "fr" : value.startsWith("ar") ? "ar" : "en";
    }

    private static String translated(String language, String english, String french, String arabic) {
        return switch (language) {
            case "fr" -> french;
            case "ar" -> arabic;
            default -> english;
        };
    }
}
