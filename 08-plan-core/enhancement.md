# Mission : Audit et optimisation des performances du moteur de planification CP-SAT

## 1. Contexte et objectif

Tu interviens sur un projet Java/Spring Boot qui utilise Google OR-Tools CP-SAT pour résoudre des problèmes de
planification.

L'objectif est de réaliser un audit technique approfondi, puis d'implémenter les améliorations nécessaires pour :

1. Réduire significativement le temps de construction du modèle CP-SAT.
2. Réduire le temps de presolve.
3. Réduire le nombre de variables, d'intervalles et de contraintes générés.
4. Améliorer la vitesse de recherche de solutions réalisables.
5. Améliorer la qualité des solutions sans dégrader excessivement les performances.
6. Éviter le blocage des consumers Kafka pendant les traitements longs.
7. Préserver la disponibilité des API REST pendant les calculs.
8. Améliorer l'observabilité et faciliter les futurs diagnostics.

**Priorité : performance globale, scalabilité et stabilité en production, sans régression fonctionnelle.**

Ne te limite pas à proposer des recommandations : implémente les améliorations justifiées par l'analyse du code.

---

## 2. Analyse du problème observé dans les logs

Le dernier benchmark présente les caractéristiques suivantes :

### 2.1. Temps de presolve CP-SAT

Le presolve prend environ 281,68 secondes avant le démarrage de la recherche.

Extrait :

```text
#Bound 281.63s best:inf next:[0,2198184] initial_domain
#Model 281.68s var:39004/39004 constraints:71431/71431
Starting deterministic search at 281.68s with 8 workers
```

Le presolve représente donc un coût majeur.

Les opérations Probe présentent notamment des durées très élevées, de l'ordre de plusieurs dizaines de secondes par
passe.

Investiguer en priorité :

- Le nombre de variables booléennes.
- Le nombre d'intervalles optionnels.
- Les contraintes conditionnelles.
- Les contraintes Linear1 générées.
- Les contraintes ExactlyOne.
- Les opérations de probing.
- Les duplications potentielles dans le modèle.
- Les combinaisons candidates générées inutilement.

### 2.2. Structure du modèle

Le modèle presolved présente approximativement :

| Élément                                |    Volume |
|----------------------------------------|----------:|
| Variables                              |    39 004 |
| Variables booléennes                   |    38 488 |
| Intervalles                            |    20 419 |
| Intervalles optionnels                 |    19 903 |
| Contraintes Linear1                    |    49 140 |
| Contraintes ExactlyOne                 |     1 728 |
| Littéraux ExactlyOne                   |    38 344 |
| NoOverlap                              |       104 |
| Entrées dans l'expansion de l'objectif | 1 258 724 |

Analyser pourquoi autant d'intervalles sont optionnels et vérifier si leur création est réellement nécessaire.

L'expansion de l'objectif est également un point d'investigation prioritaire.

### 2.3. Kafka

Un traitement long semble bloquer le consumer Kafka.

Extrait :

```text
consumer poll timeout has expired
LeaveGroup
reset generation
rejoin
```

Le traitement CP-SAT ne doit pas bloquer la boucle de polling Kafka.

### 2.4. Latence des API

Les requêtes GET du statut du PlanJob sont exécutées régulièrement.

Certaines latences augmentent pendant le calcul, avec des valeurs observées pouvant dépasser 800 ms.

Vérifier :

- La contention CPU.
- La consommation mémoire.
- Le Garbage Collector.
- Le pool de connexions JDBC.
- Les requêtes SQL.
- La fréquence de polling du frontend.
- Les traitements synchrones qui bloquent les threads applicatifs.

Ne pas considérer la base de données comme responsable sans preuve.

---

## 3. Phase 1 — Audit du code existant

Avant toute modification, explorer l'intégralité des composants concernés.

Identifier notamment :

- La construction du modèle CP-SAT.
- La génération des variables.
- La génération des intervalles.
- La génération des contraintes.
- La construction de la fonction objectif.
- Les règles de filtrage des candidats.
- Les mécanismes de hints et de warm-start.
- La configuration des workers.
- Le lancement et l'arrêt du solveur.
- Le consumer Kafka.
- Le traitement des PlanJobs.
- Les endpoints REST de consultation du statut.
- La configuration des pools de threads.
- Les configurations Spring et OR-Tools.

Produire une cartographie des dépendances et des flux d'exécution.

Pour chaque problème identifié, fournir :

- La classe et la méthode concernées.
- Le comportement actuel.
- Le coût potentiel.
- La cause démontrée ou l'hypothèse à vérifier.
- La solution proposée.
- Le risque de régression.

Ne pas modifier le code avant d'avoir compris les dépendances et les invariants métier.

---

## 4. Phase 2 — Optimisation de la construction du modèle CP-SAT

### 4.1. Réduction des candidats

Analyser la génération des combinaisons :

Task × Resource × Slot.

Mettre en place un filtrage préalable efficace pour éliminer les candidats impossibles avant leur transformation en
variables CP-SAT.

Exemples de filtrage à examiner :

- Ressource incompatible avec une tâche.
- Capacité insuffisante.
- Ressource indisponible.
- Calendrier incompatible.
- Durée incompatible avec l'horizon.
- Précédence impossible.
- Fenêtre temporelle impossible.
- Contraintes métier déjà violées.
- Candidats dominés par d'autres candidats.

Ne supprimer aucun candidat sans démontrer que son élimination préserve la validité du problème.

### 4.2. Variables

Étudier les possibilités suivantes :

- Réduire le nombre de variables booléennes.
- Éviter les variables dupliquées.
- Réutiliser les variables lorsque cela est mathématiquement correct.
- Éviter de créer des variables pour des décisions déjà déterminées.
- Réduire les domaines des variables.
- Éviter les grandes expressions intermédiaires.
- Éviter les allocations inutiles pendant la construction.

Privilégier une construction incrémentale et maîtrisée.

### 4.3. Intervalles optionnels

Analyser la création des intervalles optionnels.

Vérifier si certains intervalles peuvent être :

- Éliminés avant construction.
- Remplacés par une représentation plus simple.
- Regroupés lorsque les contraintes métier le permettent.
- Construits uniquement lorsque leur présence est nécessaire.

Préserver strictement la sémantique des contraintes NoOverlap et des intervalles conditionnels.

### 4.4. Contraintes

Étudier les possibilités de simplification :

- ExactlyOne.
- AtMostOne.
- Linear1.
- NoOverlap.
- Implications.
- Contraintes conditionnelles.
- Contraintes de précédence.
- Contraintes de capacité.
- Contraintes de calendrier.

Éviter les contraintes redondantes.

Identifier les contraintes qui peuvent être simplifiées mathématiquement avant leur ajout au modèle.

Ne pas remplacer une contrainte globale CP-SAT par des contraintes pairwise sans benchmark démontrant un avantage.

### 4.5. Fonction objectif

L'expansion de l'objectif représente environ 1,25 million d'entrées.

Analyser précisément sa construction.

Rechercher :

- Les termes dupliqués.
- Les coefficients nuls.
- Les expressions inutilement complexes.
- Les variables qui ne contribuent pas réellement à l'objectif.
- Les agrégations répétées.
- Les possibilités de pré-calcul.
- Les possibilités de simplification algébrique.

Réduire le volume de l'objectif sans modifier sa signification métier ni sa hiérarchie de priorités.

---

## 5. Phase 3 — Optimisation des paramètres CP-SAT

Analyser la configuration existante et son mapping vers les paramètres réels d'OR-Tools.

Configuration concernée :

```yaml
app:
  plan:
    cpsat:
      named-variables: false
      max-start-choices: 256
      symmetry-breaking: true
      dtime-per-second: 1.0
      progress-interval-millis: 1000
      incumbent-interval-millis: 2000
      worker-cap: 8
      log-search-progress: false
      stop-after-first-solution: true
      default-relative-gap: 0.01
      hint-conflict-limit: 100
      repair-hint: true
      default-seed: 0
      interleave-search-on-deterministic: true
```

Et les limites :

```yaml
app:
  plan:
    limits:
      max-tasks: 5000
      max-resources: 1000
      max-slots: 2000
      max-variables: 2000000
      max-horizon-ticks: 2000000
      max-candidates: 500000
      max-intervals: 1000000
      max-precedences: 500000
      max-setup-arcs: 300000
      max-pairwise: 200000
      max-requirements-per-task: 8
      max-tiers: 8
      max-calendar-runs: 100000
      max-constraint-instances: 2000000
      max-constraints: 20000
      max-request-bytes: 5000000
      max-time-seconds: 3600
      max-workers: 8
      solve-queue-timeout-seconds: 300
      timefold-unimproved-seconds: 30
```

Important :

- Vérifier le mapping effectif de chaque propriété.
- Ne pas supposer qu'une propriété est utilisée simplement parce qu'elle existe dans le YAML.
- Identifier les paramètres inutilisés ou mal interprétés.
- Éviter de multiplier les workers sans mesurer la contention CPU.
- Ne pas désactiver le symmetry breaking sans benchmark : les logs actuels ne montrent pas qu'il est le principal
  problème.
- Étudier l'impact de `max-start-choices` sur la taille du modèle.
- Vérifier le comportement de `stop-after-first-solution`.
- Vérifier le traitement du relative gap.
- Vérifier la gestion du temps maximal de résolution.
- Vérifier la gestion des conflits et des hints.

Proposer et implémenter une configuration adaptée à trois objectifs :

1. FAST_FEASIBLE : obtenir rapidement une solution réalisable.
2. BALANCED : compromis entre rapidité et qualité.
3. OPTIMAL_QUALITY : rechercher une solution de meilleure qualité dans les limites de temps disponibles.

Ne pas confondre solution réalisable, solution optimale et arrêt selon relative gap.

---

## 6. Phase 4 — Architecture asynchrone Kafka

Le consumer Kafka ne doit jamais exécuter directement un calcul CP-SAT long dans sa boucle de polling.

Étudier puis implémenter une architecture de traitement asynchrone.

Flux cible :

```text
Kafka Consumer
      |
      v
Message Validation
      |
      v
Job Registration / Persistence
      |
      v
Bounded Solver Queue
      |
      v
Dedicated Solver Executor
      |
      v
CP-SAT Solver
      |
      v
Persist Result and Status
      |
      v
Job Completion Event
```

Exigences :

- Séparer les threads Kafka des threads de calcul.
- Utiliser un executor dédié aux calculs.
- Utiliser une queue bornée.
- Éviter une croissance illimitée de la mémoire.
- Contrôler le nombre de calculs simultanés.
- Respecter `max-concurrent-solves`.
- Gérer les rejets lorsque la queue est saturée.
- Prévoir une stratégie de retry contrôlée.
- Assurer l'idempotence des messages.
- Éviter le double lancement d'un même PlanJob.
- Persister les statuts des jobs.
- Gérer les arrêts et redémarrages de l'application.
- Prévoir la récupération des jobs interrompus.
- Gérer les exceptions et les timeouts.
- Ne jamais perdre silencieusement un job.

Respecter les mécanismes d'acknowledgment Kafka existants et vérifier leur cohérence avec la persistance du job.

Ne pas augmenter simplement `max.poll.interval.ms` pour masquer le problème.

---

## 7. Phase 5 — Protection des API REST

Les API de consultation des PlanJobs doivent rester réactives pendant les calculs.

Analyser :

- Les endpoints de consultation.
- Les requêtes SQL.
- Les index.
- La pagination.
- Les transactions.
- Les accès concurrents.
- Les pools de connexions.
- Les pools de threads HTTP.

Optimiser les requêtes de statut.

Éviter de charger inutilement les objets complets lorsque seul le statut est demandé.

Étudier la possibilité de réduire la fréquence de polling du frontend, par exemple à 10–15 secondes, si cela est
compatible avec les besoins fonctionnels.

Ne pas modifier le contrat API existant sans nécessité.

---

## 8. Phase 6 — Observabilité et instrumentation

Ajouter des métriques exploitables pour chaque étape.

### Construction du modèle

Mesurer :

- model.build.duration
- model.variables.count
- model.boolean-variables.count
- model.intervals.count
- model.optional-intervals.count
- model.constraints.count
- model.candidates.generated
- model.candidates.filtered
- model.objective.terms.count

### Résolution CP-SAT

Mesurer :

- solver.presolve.duration
- solver.search.duration
- solver.total.duration
- solver.workers.count
- solver.conflicts
- solver.branches
- solver.wall-time
- solver.deterministic-time
- solver.objective.value
- solver.best-bound
- solver.relative-gap
- solver.status

### Kafka et exécution asynchrone

Mesurer :

- kafka.messages.received
- kafka.messages.acknowledged
- solver.queue.size
- solver.queue.wait.duration
- solver.concurrent.count
- solver.rejected.count
- solver.execution.duration
- solver.failed.count

### API et infrastructure

Mesurer :

- API response time.
- Database query duration.
- JDBC connection pool utilization.
- Executor utilization.
- JVM heap usage.
- GC duration.
- CPU utilization.

Utiliser les mécanismes de métriques déjà présents dans le projet, notamment Micrometer/Actuator si disponibles.

Ajouter des logs structurés avec le `planJobId` et un identifiant de corrélation.

Éviter les logs excessifs à haute fréquence.

---

## 9. Phase 7 — Tests et validation

Ajouter ou adapter les tests unitaires et d'intégration.

### Tests fonctionnels

Vérifier que les optimisations ne changent pas :

- Les règles métier.
- Les contraintes de ressources.
- Les contraintes temporelles.
- Les précédences.
- Les calendriers.
- Les priorités.
- La validité des solutions.

Comparer les résultats avant/après sur des jeux de données identiques.

### Tests de performance

Créer ou réutiliser des scénarios représentatifs :

- Petit problème.
- Problème moyen.
- Problème volumineux.
- Nombre élevé de ressources.
- Nombre élevé de candidats.
- Nombre élevé d'intervalles optionnels.
- Contraintes complexes.

Comparer :

| Indicateur           | Avant | Après |
|----------------------|------:|------:|
| Model build duration |       |       |
| Presolve duration    |       |       |
| Search duration      |       |       |
| Total duration       |       |       |
| Variables            |       |       |
| Intervals            |       |       |
| Constraints          |       |       |
| Objective terms      |       |       |
| Memory usage         |       |       |
| CPU usage            |       |       |
| API latency          |       |       |

Ne jamais annoncer un gain de performance sans benchmark réel.

### Tests Kafka

Vérifier :

- La réception des messages pendant un calcul.
- L'absence de blocage du polling.
- La gestion de la saturation de la queue.
- La reprise après redémarrage.
- L'idempotence.
- La gestion des erreurs.
- La persistance des résultats.

---

## 10. Contraintes de développement

Respecter impérativement les règles suivantes :

1. Préserver les signatures publiques des méthodes existantes autant que possible.
2. Ne supprimer aucune méthode existante sans justification explicite.
3. Ne pas modifier les contrats API inutilement.
4. Respecter les conventions de nommage et l'architecture existantes.
5. Réutiliser les abstractions déjà présentes.
6. Éviter les nouvelles dépendances si elles ne sont pas nécessaires.
7. Éviter les optimisations prématurées.
8. Ne pas introduire de variables globales mutables.
9. Respecter la sécurité des traitements concurrents.
10. Respecter les transactions et la gestion des ressources.
11. Ajouter des tests pour chaque évolution importante.
12. Documenter les décisions techniques.
13. Conserver la compatibilité avec les configurations existantes.
14. Ne pas introduire de breaking changes sans justification.

Pour chaque modification, vérifier les effets sur les autres composants.

---

## 11. Méthode de travail attendue

Procéder par étapes.

### Étape A — Audit

Explorer le code et identifier les causes réelles des problèmes.

Livrer une synthèse avec les fichiers et méthodes concernés.

### Étape B — Plan d'amélioration

Classer les optimisations par priorité :

- P0 : blocage Kafka, stabilité et sécurité des traitements.
- P1 : réduction du temps de construction et du presolve.
- P2 : optimisation de l'objectif et des contraintes.
- P3 : amélioration de la recherche et de la qualité des solutions.
- P4 : observabilité et optimisation complémentaire.

Identifier les risques et les dépendances.

### Étape C — Implémentation

Implémenter les optimisations par lots cohérents.

Ne pas faire un refactoring massif sans possibilité d'isoler les régressions.

### Étape D — Validation

Exécuter :

- Compilation.
- Tests unitaires.
- Tests d'intégration.
- Tests fonctionnels.
- Benchmarks disponibles.

Corriger les régressions avant de poursuivre.

### Étape E — Rapport final

Fournir un rapport contenant :

1. Les problèmes identifiés.
2. Les causes confirmées.
3. Les classes et méthodes modifiées.
4. Les optimisations réalisées.
5. Les paramètres de configuration modifiés.
6. Les nouveaux paramètres éventuellement introduits.
7. Les résultats des tests.
8. Les résultats des benchmarks avant/après.
9. Les points restant à investiguer.
10. Les recommandations pour la production.

---

## 12. Critères de réussite

L'évolution sera considérée comme réussie si :

- Le temps de construction du modèle diminue.
- Le temps de presolve diminue significativement.
- Le nombre de candidats inutiles diminue.
- Le nombre de variables et d'intervalles est mieux maîtrisé.
- La fonction objectif est moins coûteuse à construire.
- Le consumer Kafka reste opérationnel pendant les calculs.
- Les API REST restent réactives.
- Les jobs sont récupérables après redémarrage.
- Les résultats restent conformes aux règles métier.
- Les tests fonctionnels et d'intégration passent.

**Consigne finale : commence par comprendre le code réel. Ne suppose pas que les hypothèses issues des logs sont
nécessairement des bugs. Identifie les causes dans l'implémentation, mesure les performances, puis optimise.**

Ne te contente pas d'un rapport d'analyse : après l'audit, réalise effectivement les évolutions justifiées, teste-les et
présente un bilan précis des résultats.