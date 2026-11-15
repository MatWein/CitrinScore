package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object GreekStrings : Strings {
    override val next = "Επόμενο"
    override val back = "Πίσω"
    override val getStarted = "Ας ξεκινήσουμε"
    override fun stepOf(current: Int, total: Int) = "Βήμα $current από $total"

    override val welcomeTitle = "Καλώς ήρθες στο ${AppInfo.NAME}"
    override val welcomeSubtitle =
        "Η προσωπική σου βιβλιοθήκη παρτιτούρων – οργανωμένη, με σημειώσεις και έτοιμη για αναπαραγωγή."

    override val featureLibraryTitle = "Εισαγωγή όλων"
    override val featureLibraryText = "Εικόνες, PDF και MusicXML σε ένα μέρος."
    override val featureNotesTitle = "Σημειώσεις"
    override val featureNotesText = "Σχολίασε τις παρτιτούρες σου με το χέρι ή με κείμενο."
    override val featurePlaybackTitle = "Ακρόαση"
    override val featurePlaybackText = "Αναπαραγωγή παρτιτούρων MusicXML με ταυτόχρονη παρακολούθηση."

    override val languageTitle = "Επίλεξε γλώσσα"
    override val languageSubtitle = "Μπορείς να την αλλάξεις αργότερα στις ρυθμίσεις."

    override val libraryLocationTitle = "Πού θα βρίσκεται η βιβλιοθήκη σου;"
    override val libraryLocationSubtitle =
        "Επίλεξε έναν φάκελο για τις παρτιτούρες σου. Όλα αποθηκεύονται εκεί ως κανονικά αρχεία, ώστε να μπορείς να τα αντιγράψεις ή να τα μετακινήσεις ανά πάσα στιγμή."
    override val chooseFolder = "Επιλογή φακέλου"
    override val changeFolder = "Αλλαγή φακέλου"
    override val folderPickerFailed = "Δεν ήταν δυνατό το άνοιγμα του φακέλου. Δοκίμασε ξανά."

    override val libraryTitle = "Βιβλιοθήκη"
    override val libraryEmptyTitle = "Η βιβλιοθήκη σου είναι άδεια"
    override val libraryEmptyText = "Οι παρτιτούρες που εισάγεις θα εμφανίζονται εδώ."

    override val cancel = "Ακύρωση"
    override val create = "Δημιουργία"

    override val newFolder = "Νέος φάκελος"
    override val importScores = "Εισαγωγή παρτιτούρων"
    override val newFolderTitle = "Δημιουργία νέου φακέλου"
    override val folderNameLabel = "Όνομα φακέλου"
    override val nameEmpty = "Πληκτρολόγησε ένα όνομα."
    override val nameInvalid = "Το όνομα δεν μπορεί να περιέχει κανέναν από αυτούς τους χαρακτήρες: \\ / : * ? \" < > |"
    override val nameExists = "Αυτό το όνομα υπάρχει ήδη σε αυτόν τον φάκελο."

    override val importTypeTitle = "Τι θέλεις να εισαγάγεις;"
    override val importTypeSubtitle = "Επίλεξε τη μορφή των παρτιτούρων σου."
    override val importTypeImages = "Εικόνες"
    override val importTypeImagesText = "Μία ή περισσότερες εικόνες, π.χ. σαρωμένες σελίδες (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Ένα μεμονωμένο έγγραφο PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Ψηφιακές παρτιτούρες που μπορούν και να αναπαραχθούν."
    override val comingSoon = "Έρχεται σύντομα"

    override val importFilesTitle = "Επίλεξε εικόνες"
    override val importFilesSubtitle = "Βάλε τις σελίδες στη σωστή σειρά."
    override val addImages = "Προσθήκη εικόνων"
    override val moveUp = "Μετακίνηση πάνω"
    override val moveDown = "Μετακίνηση κάτω"
    override val remove = "Αφαίρεση"

    override val importPreviewTitle = "Προεπισκόπηση"
    override val importPreviewSubtitle = "Έτσι θα φαίνεται η παρτιτούρα σου."
    override val previewFailed = "Δεν ήταν δυνατή η φόρτωση των εικόνων."

    override val importMetadataTitle = "Λεπτομέρειες"
    override val importMetadataSubtitle = "Δώσε έναν τίτλο στην παρτιτούρα σου για να τη βρίσκεις αργότερα."
    override val titleLabel = "Τίτλος"
    override val composerLabel = "Συνθέτης"
    override val descriptionLabel = "Περιγραφή"
    override val importAction = "Εισαγωγή"
    override val importFailed = "Δεν ήταν δυνατή η αποθήκευση της παρτιτούρας. Δοκίμασε ξανά."
}
