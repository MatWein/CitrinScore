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
}
