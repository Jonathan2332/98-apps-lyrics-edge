package a98apps.lyricsedge.util

class FormatterUtil
{
    companion object
    {
        private const val NEW_LINE = "\n"
        private const val SPACE = " "

        fun formatSpaceText(text: Any, formattedText: String): String
        {
            return text.toString() + SPACE + formattedText
        }

        fun formatNewLine(text: Any, formattedText: String): String
        {
            return text.toString() + NEW_LINE + formattedText
        }
    }
}