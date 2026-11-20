package de.citrinscore.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import de.citrinscore.score.playback.Instrument

interface Strings {
    val next: String
    val back: String
    val getStarted: String
    fun stepOf(current: Int, total: Int): String

    val welcomeTitle: String
    val welcomeSubtitle: String
    val featureImportTitle: String
    val featureImportText: String
    val featureScanTitle: String
    val featureScanText: String
    val featureNotesTitle: String
    val featureNotesText: String
    val featurePlaybackTitle: String
    val featurePlaybackText: String
    val featureRecordingTitle: String
    val featureRecordingText: String
    val featureSyncTitle: String
    val featureSyncText: String

    val languageTitle: String
    val languageSubtitle: String

    val themeTitle: String
    val themeSubtitle: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String

    val settings: String
    val settingsLanguage: String
    val settingsTheme: String
    val settingsLibrary: String

    val libraryLocationTitle: String
    val libraryLocationSubtitle: String
    val chooseFolder: String
    val libraryInApp: String
    val libraryInAppText: String
    val libraryOwnFolder: String
    val libraryOwnFolderText: String
    val moveLibraryTitle: String
    fun moveLibraryText(target: String, stopsSync: Boolean): String
    val moveLibrary: String
    val movingLibrary: String
    val libraryMoveOccupied: String
    val libraryMoveFailed: String
    val libraryMoveSourceKept: String
    val changeFolder: String
    val folderPickerFailed: String
    val libraryUnavailableTitle: String
    fun libraryUnavailableText(folder: String): String
    val retry: String
    val chooseOtherFolder: String
    val libraryStillUnavailable: String

    val libraryTitle: String
    val libraryEmptyTitle: String
    val folderEmptyTitle: String
    val libraryEmptyText: String

    val cancel: String
    val create: String

    val refresh: String
    val search: String
    val searchPlaceholder: String
    val closeSearch: String
    val noSearchResults: String
    val sort: String
    val sortRecentlyUsed: String
    val sortTitle: String
    val moreOptions: String
    val delete: String
    val deleteTitle: String
    fun deleteScoreText(name: String): String
    fun deleteFolderText(name: String): String
    val deleteFailed: String
    val export: String
    val exportFailed: String
    val createFolderFailed: String
    val folderLoadFailed: String
    val settingsSaveFailed: String

    val newFolder: String
    val importScores: String
    val newFolderTitle: String
    val folderNameLabel: String
    val nameEmpty: String
    val nameInvalid: String
    val nameReservedExtension: String
    val nameExists: String


    val importFilesTitle: String
    val importFilesSubtitle: String
    val addFiles: String
    val musicXmlLabel: String
    val takePhoto: String
    val scanThreshold: String
    val scanCrop: String
    val scanEraser: String
    val eraserSize: String
    val moveUp: String
    val moveDown: String
    val remove: String

    val previewLabel: String
    val previewFailed: String
    val omrCreatePart: String
    val omrPreviewLabel: String
    val omrRecognizing: String
    val omrFailed: String
    val omrPartSummary: String

    val importMetadataTitle: String
    val importMetadataSubtitle: String
    val titleLabel: String
    val composerLabel: String
    val descriptionLabel: String
    val importAction: String
    val importFailed: String
    val edit: String
    val editDetailsTitle: String
    val editDetailsSubtitle: String
    val save: String
    fun pageNumber(number: Int): String
    val editLoadFailed: String
    val parts: String
    val displayOriginal: String
    val displayNormalized: String
    val displayMusicXml: String
    fun partNumber(number: Int): String
    val partNameLabel: String
    val partNameExists: String
    val importPartsSubtitle: String
    val editPartsSubtitle: String
    val addPart: String
    fun pageCount(count: Int): String
    val editSaveFailed: String

    val zoom: String
    fun zoomPercent(percent: Int): String
    val previousPage: String
    val nextPage: String
    val audio: String
    val noAudio: String
    val addAudioFile: String
    val startRecording: String
    val stopRecording: String
    val recordingActive: String
    val recordingTitle: String
    fun recordingLength(length: String): String
    val saveRecording: String
    val discardRecording: String
    fun recordingName(number: Int): String
    val renameTitle: String
    val nameLabel: String
    val rename: String
    fun deleteAttachmentText(name: String): String
    val play: String
    val pause: String
    val stop: String
    val closePlayer: String
    val audioFailedTitle: String
    val audioPlaybackFailed: String
    val audioRecordingFailed: String
    val audioSaveFailed: String
    val ok: String
    val annotate: String
    val attachments: String
    val noAttachments: String
    val attachFile: String
    val saveAs: String
    val attachmentFailed: String
    val annotationPen: String
    val annotationText: String
    val annotationEraser: String
    val undo: String
    val done: String
    val textNoteTitle: String
    val textNoteLabel: String
    val annotationsFailedTitle: String
    val annotationsSaveFailed: String
    val scoreLoadFailed: String
    val playback: String
    val closePlayback: String
    val listenMode: String
    val playAlongMode: String
    val startPlayAlong: String
    val stopPlayAlong: String
    val tempo: String
    val playbackScoreFailed: String
    val playbackSynthesizerFailed: String
    val playbackMicrophoneFailed: String
    val playAlongHint: String
    fun tempoPercent(percent: Int): String
    val instrument: String
    val notatedInstrument: String
    fun instrumentName(instrument: Instrument): String
    val sync: String
    val moreActions: String
    val settingsSync: String
    val settingsAbout: String
    val aboutTitle: String
    fun appVersion(version: String): String
    fun buildDate(date: String): String
    fun createdBy(author: String): String
    val sourceCode: String
    val license: String
    val licenseText: String
    val openSourceLibraries: String
    val openSourceLibrariesText: String
    val showLicenses: String
    val website: String
    val viewLicense: String
    val syncSettingsHint: String
    val gitUrl: String
    val gitUsername: String
    val gitToken: String
    val connect: String
    val disconnect: String
    val syncCommitting: String
    val syncDownloading: String
    val syncMerging: String
    val syncUploading: String
    val syncUpToDate: String
    val syncCancelled: String
    val syncConflictsTitle: String
    val syncConflictsText: String
    val keepLocal: String
    val keepRemote: String
    val continueSync: String
    val cancelSync: String
    val syncLibraryLocation: String
    val syncConnection: String
    val syncRejected: String
    val syncFailed: String
    fun syncConnectedTo(url: String): String
    fun syncCompleted(uploaded: Int, downloaded: Int): String
    fun syncWithChanges(count: Int): String
    fun localChanges(count: Int): String
    val discardChanges: String
    val discardChangesTitle: String
    val discardChangesText: String
    val discard: String
    val changesDiscarded: String
    val discardFailed: String
}

val LocalStrings = staticCompositionLocalOf { AppLanguage.English.strings }
