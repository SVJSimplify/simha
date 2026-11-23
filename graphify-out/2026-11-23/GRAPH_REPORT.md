# Graph Report - Simha  (2026-11-23)

## Corpus Check
- 83 files · ~50,130 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 57 file(s) not represented in the graph (top: .xml 53, (none) 1, .ttf 1)
- WARNING: 57 of 148 files seen (39%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 1601 nodes · 4488 edges · 69 communities (36 shown, 33 thin omitted)
- Extraction: 95% EXTRACTED · 5% INFERRED · 0% AMBIGUOUS · INFERRED: 219 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `00501794`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- MainViewModel
- FakeHost
- Repo
- TalkViewModel.kt
- Actions
- Voice.kt
- PlansTest
- Components.kt
- .decide
- with
- ActivityService.kt
- Wake.kt
- DoseLog
- MainViewModel
- Theme.kt
- Onboarding.kt
- Home.kt
- SarvamConfig
- VoiceIssue
- build
- App.kt
- SarvamHealthTest.kt
- .execute
- MainViewModel.kt
- Situation
- Weather
- FallDetector.kt
- .learn
- SimhaOrb.kt
- Voice
- .run
- MedicinesScreen.kt
- UpdateState.kt
- ParsedReply
- SafeArea.kt
- MsgRecorder
- Perm
- .check
- Weather.kt
- Updater.kt
- VoiceAgent.kt
- ContactInfo
- LlmStore
- UsageReader.kt
- DiaryMerge
- .parseTts
- UtteranceProgressListener
- Scam
- .chat
- TalkMode
- RecPhase
- Context

## God Nodes (most connected - your core abstractions)
1. `MainViewModel` - 65 edges
2. `jo()` - 56 edges
3. `Repo` - 50 edges
4. `SettingsScreen()` - 43 edges
5. `Wake` - 38 edges
6. `ToolHost` - 34 edges
7. `FakeHost` - 30 edges
8. `Body()` - 30 edges
9. `HomeScreen()` - 29 edges
10. `Voice` - 27 edges

## Surprising Connections (you probably didn't know these)
- `SimhaOrb()` --calls--> `Stroke`  [INFERRED]
  app/src/main/java/com/simha/app/ui/home/SimhaOrb.kt → app/src/main/java/com/simha/app/logic/FirstAid.kt
- `SimhaRoot()` --calls--> `ProvideLayout()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `animMs()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Components.kt
- `AppNav()` --calls--> `HomeScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Home.kt
- `AppNav()` --calls--> `OnboardingScreen()`  [INFERRED]
  app/src/main/java/com/simha/app/ui/App.kt → app/src/main/java/com/simha/app/ui/Onboarding.kt

## Import Cycles
- None detected.

## Communities (69 total, 33 thin omitted)

### Community 0 - "MainViewModel"
Cohesion: 0.24
Nodes (23): Snap, AppNav(), AuditScreen(), CaregiverHomeScreen(), CaregiverSetupScreen(), eventLabel(), shortTime(), VoiceMessageCard() (+15 more)

### Community 2 - "Repo"
Cohesion: 0.06
Nodes (17): ContactEntity, DiaryEntity, DoseLogEntity, GuardianDao, MedicineEntity, MemoryEntity, OutboxEntity, ProfileEntity (+9 more)

### Community 3 - "TalkViewModel.kt"
Cohesion: 0.12
Nodes (7): TalkScreen(), ChatLine, Prompt, TalkEvent, OpenEmergency, TalkViewModel, UiMsg

### Community 5 - "Voice.kt"
Cohesion: 0.09
Nodes (6): RecognitionListener, VoiceFail, VoiceKind, Failed, NoSpeech, Unavailable

### Community 6 - "PlansTest"
Cohesion: 0.07
Nodes (24): Ago, Days, Hours, Minutes, Never, Now, Answer, Fine (+16 more)

### Community 7 - "Components.kt"
Cohesion: 0.07
Nodes (21): animMs(), BigButton(), ButtonCore(), Chip(), ChipGroup(), EmergencyButton(), Grid(), Kind (+13 more)

### Community 8 - ".decide"
Cohesion: 0.17
Nodes (9): Approvals, Decision, AlertFamily, EscalateCallFamily, None, PromptCheckin, Rules, RulesEngine (+1 more)

### Community 9 - "with"
Cohesion: 0.06
Nodes (33): runs-on, steps, concurrency, cancel-in-progress, group, env, KEYSTORE_BASE64, VERSION_CODE (+25 more)

### Community 10 - "ActivityService.kt"
Cohesion: 0.05
Nodes (9): forLang(), ActivityService, Notifier, ActivityWorker, ReminderWorker, SyncWorker, Work, SimhaApp (+1 more)

### Community 12 - "Wake.kt"
Cohesion: 0.07
Nodes (9): Monitor, AlarmReceiver, BootReceiver, Wake, WakeAudio, WakeActivity, RoutineStore, WakeRequest (+1 more)

### Community 13 - "DoseLog"
Cohesion: 0.06
Nodes (37): AuditRow, CallRow, CaregiverRow, Diary, DiaryRow, DoseLog, DoseRow, EventRow (+29 more)

### Community 18 - "Theme.kt"
Cohesion: 0.07
Nodes (5): MainActivity, SimhaColors, nunito(), scheme(), SimhaTheme()

### Community 19 - "Onboarding.kt"
Cohesion: 0.06
Nodes (17): OBK, Step, Layout, line(), medLine(), showTime(), ChoiceCard(), OnboardingScreen() (+9 more)

### Community 20 - "Home.kt"
Cohesion: 0.09
Nodes (9): TodayItem, TodayState, agoText(), ActionTile(), HomeScreen(), TodayRow(), todayText(), showMillis() (+1 more)

### Community 21 - "SarvamConfig"
Cohesion: 0.22
Nodes (4): SarvamConfig, Sarvam, SarvamException, SarvamClient

### Community 22 - "VoiceIssue"
Cohesion: 0.50
Nodes (4): VoiceIssue, LangMissing, None, Unavailable

### Community 26 - "SarvamHealthTest.kt"
Cohesion: 0.13
Nodes (7): HealthMemory, Endpointer, EpState, Done, Timeout, Waiting, SarvamHealthTest

### Community 29 - ".execute"
Cohesion: 0.04
Nodes (4): SimhaTools, ToolHost, Host, HealthHost

### Community 30 - "MainViewModel.kt"
Cohesion: 0.06
Nodes (11): K, Prefs, toUi(), UiKeys, UiSettings, startWakeWord(), stopWakeWord(), WakeWordEngine (+3 more)

### Community 31 - "Situation"
Cohesion: 0.06
Nodes (32): Guardian, GuardianContext, MedLine, FirstAid, Guide, MedicalProfile, Situation, Allergy (+24 more)

### Community 32 - "Weather"
Cohesion: 0.15
Nodes (4): Weather, WeatherReport, WakeMath, WakeMathTest

### Community 33 - "FallDetector.kt"
Cohesion: 0.13
Nodes (7): FallDetector, FallMachine, Phase, Free, Idle, Impact, FallMachineTest

### Community 34 - ".learn"
Cohesion: 0.20
Nodes (3): NightState, Routine, RoutineTest

### Community 35 - "SimhaOrb.kt"
Cohesion: 0.09
Nodes (7): Stroke, OrbState, Idle, Listening, Speaking, Thinking, SimhaOrb()

### Community 36 - "Voice"
Cohesion: 0.17
Nodes (3): Recorder, SpeakItem, Voice

### Community 37 - ".run"
Cohesion: 0.18
Nodes (5): Agent, ChatApi, History, Msg, SentenceSplitter

### Community 38 - "MedicinesScreen.kt"
Cohesion: 0.13
Nodes (5): doseStatusText(), MedicineForm(), MedicinesScreen(), TimeDialog(), TimeTag()

### Community 39 - "UpdateState.kt"
Cohesion: 0.15
Nodes (3): UpdateInstallReceiver, UpdateState, UpdateUi

### Community 40 - "ParsedReply"
Cohesion: 0.16
Nodes (10): LlmClient, LlmConfig, LlmException, LlmParse, ParsedReply, StreamAcc, CallAcc, ToolCall (+2 more)

### Community 44 - "Perm"
Cohesion: 0.17
Nodes (11): Perm, Battery, CallPhone, Camera, FullScreen, Mic, Notifications, Usage (+3 more)

### Community 48 - "Weather.kt"
Cohesion: 0.13
Nodes (4): CallResult, Called, Dialer, Failed

### Community 49 - "Updater.kt"
Cohesion: 0.16
Nodes (3): UpdateException, UpdateInfo, Updater

### Community 50 - "VoiceAgent.kt"
Cohesion: 0.24
Nodes (5): Escalation, AgentCall, AgentNotConfiguredException, SarvamVoiceAgent, VoiceAgent

### Community 58 - "Scam"
Cohesion: 0.50
Nodes (3): Scam, Sig, ScamReport

### Community 60 - "TalkMode"
Cohesion: 0.40
Nodes (5): TalkMode, Idle, Listening, Speaking, Thinking

### Community 61 - "RecPhase"
Cohesion: 0.50
Nodes (4): RecPhase, Idle, Ready, Recording

## Knowledge Gaps
- **85 isolated node(s):** `Allergy`, `Anaphylaxis`, `Fall`, `ChestPain`, `LowSugar` (+80 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 301 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **33 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `jo()` connect `Repo` to `MainViewModel`, `FallDetector.kt`, `TalkViewModel.kt`, `SafeArea.kt`, `Monitor.kt`, `ActivityService.kt`, `Wake.kt`, `DoseLog`, `.check`, `MainViewModel`, `Weather.kt`, `VoiceAgent.kt`, `Onboarding.kt`, `.chat`, `.execute`, `MainViewModel.kt`?**
  _High betweenness centrality (0.078) - this node is a cross-community bridge._
- **Are the 23 inferred relationships involving `jo()` (e.g. with `.addMemory()` and `.deleteMedicine()`) actually correct?**
  _`jo()` has 23 INFERRED edges - model-reasoned connections that need verification._
- **What connects `Allergy`, `Anaphylaxis`, `Fall` to the rest of the system?**
  _85 weakly-connected nodes found - possible missing edges._
- **Should `Repo` be split into smaller, more focused modules?**
  _Cohesion score 0.05625790139064475 - nodes in this community are weakly interconnected._
- **Why does `Repo` connect `Repo` to `TalkViewModel.kt`, `Voice`, `Voice.kt`, `SafeArea.kt`, `Monitor.kt`, `ActivityService.kt`, `Onboarding.kt`, `Home.kt`, `MainViewModel.kt`?**
  _High betweenness centrality (0.074) - this node is a cross-community bridge._
- **Are the 12 inferred relationships involving `SettingsScreen()` (e.g. with `AppNav()` and `.remember()`) actually correct?**
  _`SettingsScreen()` has 12 INFERRED edges - model-reasoned connections that need verification._
- **Should `TalkViewModel.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.11806543385490754 - nodes in this community are weakly interconnected._