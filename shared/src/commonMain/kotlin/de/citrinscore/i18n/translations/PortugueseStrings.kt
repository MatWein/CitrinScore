package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object PortugueseStrings : Strings {
    override val next = "Avançar"
    override val back = "Voltar"
    override val getStarted = "Começar"
    override fun stepOf(current: Int, total: Int) = "Passo $current de $total"

    override val welcomeTitle = "Boas-vindas ao ${AppInfo.NAME}"
    override val welcomeSubtitle = "A sua biblioteca pessoal de partituras – organizada, anotada e pronta para tocar."

    override val featureLibraryTitle = "Importe tudo"
    override val featureLibraryText = "Imagens, PDF e MusicXML reunidos num só lugar."
    override val featureNotesTitle = "Faça anotações"
    override val featureNotesText = "Anote as suas partituras à mão ou com texto."
    override val featurePlaybackTitle = "Ouça"
    override val featurePlaybackText = "Reproduza partituras MusicXML e acompanhe a leitura."

    override val languageTitle = "Escolha o seu idioma"
    override val languageSubtitle = "Pode alterá-lo mais tarde nas definições."

    override val libraryLocationTitle = "Onde deve ficar a sua biblioteca?"
    override val libraryLocationSubtitle =
        "Escolha uma pasta para as suas partituras. Tudo é guardado lá como ficheiros normais, para que possa fazer cópias de segurança ou movê-los a qualquer momento."
    override val chooseFolder = "Escolher pasta"
    override val changeFolder = "Alterar pasta"
    override val folderPickerFailed = "Não foi possível abrir a pasta. Tente novamente."

    override val libraryTitle = "Biblioteca"
    override val libraryEmptyTitle = "A sua biblioteca está vazia"
    override val libraryEmptyText = "As partituras importadas aparecerão aqui."
}
