# Copilot Agents en Skills om Overheid transitie te ondersteunen
Diverse organisaties zijn momenteel druk bezig met het faceliften van legacy applicaties naar de laatste stand van technology (JDK, Spring framework en boot) en de move
van bare-metal RHEL VMs naar Kubernetes. Het lijkt het mij wenselijk het proces te versnellen door het inzetten van AI assisted software engineering ([Intellij Copilot](https://dev.to/eddybenchek/what-i-learned-building-with-github-copilot-as-a-java-developer-9bk)).
Dit versnelt niet alleen het proces maar verbetert ook de kwaliteit van het eind product. Dit project bevat custom AI Copilot Agents en Skills ter ondersteuning van [Modernisering Legacy Systemen](#modernisering-legacy-systemen)

## Verwarring omtrent de move naar Azure in relatie tot NIS2 richtlijnen

[NIS2-richtlijn](https://stackit.com/en/learn/knowledge/nis2) (in Nederland de Cyberbeveiligingswet) is op 15 augustus 2026 officieel in werking getreden. Deze wetgeving is relevanter dan ooit omdat de cybersecurity de keuze tussen US scalers en EU gehoste cloud providers niet langer een vrijblijvende IT-keuze is, maar een wettelijke bestuursverantwoordelijkheid met zware sancties.
De relatie met soevereine clouds zoals [STACKIT](https://coding.agency/kennisbank/kpn-stackit-soevereine-cloud-nederland#waarom-de-overheid-niet-meer-alleen-op-aws-en-azure-wil-leunen) is direct en cruciaal: NIS2 dwingt organisaties om grip te krijgen op hun digitale toeleveringsketen, en een soevereine cloud biedt daarvoor het juridische en technische fundament.

Ik begrijp dan ook niet waarom men geld en energie investeert in het verhuizen van bare-metal VM gehoste oplossingen naar Azure AKS.

**Note:** Lees het recentelijke NDD publicatie NDD [Mijlpaal Doorbraakproject: "Het Fundament" (De Soevereine Cloud)](https://www.digitaleoverheid.nl/nieuws-nds/nds-cloud-mijlpaal-publicatie-van-het-ontwerp/)

## Huidige juridische status in Nederland (Cyberbeveiligingswet)

De Europese NIS2-richtlijn (deadline oktober 2024) zijn in Nederland officieel omgezet in nationale wetgeving via de Cyberbeveiligingswet (Cbw).
Na goedkeuring door de Tweede en Eerste Kamer is deze wet op 15 augustus 2026 officieel in werking getreden, zonder overgangsperiode.
Sinds die datum moeten ruim 8.000 organisaties in Nederland – waaronder **overheidsinstanties** – wettelijk voldoen aan strenge zorg- en meldplichten.
Grote Amerikaanse cloudaanbieders (waaronder MicroSoft met Azure) vallen onder de Amerikaanse CLOUD Act, waarmee de Amerikaanse overheid in theorie data kan opvorderen. Binnen de NIS2-kaders en de vernieuwde Baseline Informatiebeveiliging Overheid vormt dit een direct compliance-risico voor gevoelige data.

De overheid gebruikt de implementatie van NIS2 dus als het fundament om te eisen dat vitale overheidsapplicaties uitsluitend nog draaien op infrastructuren die volledig immuun zijn voor extraterritoriale claims.

## Modernisering Legacy Systemen

Migratie werk kan bestaan uit de volgende onderdelen:
- Een verouderde java applicatie met java versie 8 moet vernieuwd worden naar de laatste lts-versie;
- De gehele OTAP draait op een of meer RHEL-servers in de kelder van het hoofdgebouw van de klant en moet naar Azure (AKS) worden gebracht;
- Men twijfelt nog over de inzet van OpenShift;
- De CI/CD is een Jenkins pipeline en die moet naar Azure pipelines worden omgezet;
- Een nieuw framework worden geselecteerd zoals bijvoorbeeld Spring of Quarkus;
- De bestaande unit en integration tests zinvol maken.

## Copilot Architectuur Agents
Ik heb gekozen om niet één generieke “migration agent” te maken, maar een orchestrator-agent met gespecialiseerde migration skills/instructions. Dat sluit heel goed aan bij de problematiek van het migreren van legacy applicaties en de move van bare metal RHEL VMs naar Kubernetes cloud: Java 8 → actuele LTS, RHEL/on-prem → Azure/AKS, OpenShift-evaluatie, Jenkins → Azure Pipelines, frameworkselectie en het verbeteren van unit/integration tests.
```
                         ┌──────────────────────────┐
                         │ java-migration-orchestrator│
                         │          agent            │
                         └────────────┬─────────────┘
                                      │
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
              ▼                       ▼                       ▼
     ┌────────────────┐      ┌────────────────┐      ┌────────────────┐
     │ Java Migration │      │ Cloud / K8s    │      │ CI/CD Migration│
     │     Skill      │      │     Skill      │      │     Skill      │
     └────────────────┘      └────────────────┘      └────────────────┘
              │                       │                       │
       Java 8 → LTS             RHEL → AKS             Jenkins → Azure
       Spring upgrade            OpenShift               Pipelines
       dependencies             Docker/Jib               quality gates
       deprecated APIs           Helm                     artifacts
       
              ┌───────────────────────┼───────────────────────┐
              │                       │                       │
              ▼                       ▼                       ▼
     ┌────────────────┐      ┌────────────────┐      ┌────────────────┐
     │ Test Migration │      │ Framework      │      │ Modernization  │
     │     Skill      │      │ Evaluation     │      │     Review     │
     └────────────────┘      └────────────────┘      └────────────────┘
              │                       │                       │
       JUnit 5                  Spring vs Quarkus       architecture
       integration              criteria                 security
       Testcontainers            PoC                      observability
       test quality              ADR                      twelve-factor
```
De werkzaamheden kunnen bestaan uit Java 8 naar de laatste LTS, RHEL naar Azure AKS, OpenShift, Jenkins naar Azure Pipelines, frameworkselectie en het verbeteren van unit- en integration-tests.

## 10 concrete use-case prompts
Zie prompts onder directory [.github/prompts](.github/prompts/README.md). Deze prompts zijn bedoeld om de migration agent te instrueren en te begeleiden bij het uitvoeren van de migratie. De prompts zijn als volgt:
1. Analyseer de bestaande Jenkins pipeline en maak een mapping naar Azure DevOps.
2. Ontwerp een `azure-pipelines.yml` met minimaal checkout, build, unit tests
3. Ontwerp een `azure-pipelines.yml` met minimaal checkout, build, unit tests, integration tests, package, Jib/container build, quality/security checks, push naar ACR, deployment naar AKS en smoke/health validation.
4. Maak een migration checklist voor Jenkins → Azure Pipelines.
5. Analyseer de bestaande Java 8 applicatie en maak een plan voor upgrade naar de laatste LTS-versie van Java.
6. Evalueer de huidige RHEL-servers en maak een plan voor migratie naar Azure AKS.
7. Onderzoek de mogelijkheid om OpenShift te gebruiken en maak een aanbeveling.
8. Analyseer de bestaande unit- en integration-tests en maak een plan om deze te verbeteren en te moderniseren.
9. Evalueer de huidige framework en maak een aanbeveling voor een nieuw framework zoals Spring of Quarkus.
10. Ontwerp een plan voor het verbeteren van de architectuur, security, observability en het implementeren van twelve-factor app principes.

Alle prompts volgen de volgende strategie:

Assess → Baseline → Plan → Execute → Test → Deploy → Validate → Report

