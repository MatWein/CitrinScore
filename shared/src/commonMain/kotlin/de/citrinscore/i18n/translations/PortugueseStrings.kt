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

    override val cancel = "Cancelar"
    override val create = "Criar"

    override val newFolder = "Nova pasta"
    override val importScores = "Importar partituras"
    override val newFolderTitle = "Criar nova pasta"
    override val folderNameLabel = "Nome da pasta"
    override val nameEmpty = "Introduza um nome."
    override val nameInvalid = "O nome não pode conter nenhum destes caracteres: \\ / : * ? \" < > |"
    override val nameExists = "Este nome já existe nesta pasta."

    override val importTypeTitle = "O que pretende importar?"
    override val importTypeSubtitle = "Escolha o formato das suas partituras."
    override val importTypeImages = "Imagens"
    override val importTypeImagesText = "Uma ou mais imagens, p. ex. páginas digitalizadas (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Um único documento PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Partituras digitais que também podem ser reproduzidas."
    override val comingSoon = "Em breve"

    override val importFilesTitle = "Escolha as suas imagens"
    override val importFilesSubtitle = "Coloque as páginas pela ordem certa."
    override val addImages = "Adicionar imagens"
    override val moveUp = "Mover para cima"
    override val moveDown = "Mover para baixo"
    override val remove = "Remover"

    override val importPreviewTitle = "Pré-visualização"
    override val importPreviewSubtitle = "É assim que a sua partitura vai ficar."
    override val previewFailed = "Não foi possível carregar as imagens."

    override val importMetadataTitle = "Detalhes"
    override val importMetadataSubtitle = "Dê um título à sua partitura para a encontrar mais tarde."
    override val titleLabel = "Título"
    override val composerLabel = "Compositor"
    override val descriptionLabel = "Descrição"
    override val importAction = "Importar"
    override val importFailed = "Não foi possível guardar a partitura. Tente novamente."
}
