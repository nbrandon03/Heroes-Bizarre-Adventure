package Application.Dialogue;

// IMPORTS
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple dialogue manager that reads lines from a text file.
 * Once it reaches the end, it returns an empty string on further calls
 * (no looping back to the beginning).
 *
 * @author Nikolas Kiroff
 */
public class DialogueManager {

    private final List<String> lines = new ArrayList<>();
    private int index = -1; // start BEFORE the first line

    /**
     * @param filePath path to the dialogue text file (e.g. "src/Assets/Dialogue/castle.txt")
     */
    public DialogueManager(String filePath) {
        try {
            List<String> raw = Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
            for (String s : raw) {
                String trimmed = s.trim();
                // Ignore empty lines and comment lines if you want
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    lines.add(trimmed);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // if read fails, lines stays empty and we'll just return ""
        }
    }

    /**
     * Returns the next dialogue line.
     * - If there are no lines or we're past the end, returns "".
     * - Does NOT loop back to the beginning.
     */
    public String getNextLine() {
        if (lines.isEmpty()) {
            return "";
        }
        // If we’re already at or past the last line, stay "finished"
        if (index + 1 >= lines.size()) {
            return "";
        }
        index++;
        return lines.get(index);
    }

    /**
     * @return true if there is at least one more line available.
     */
    public boolean hasNext() {
        return !lines.isEmpty() && (index + 1) < lines.size();
    }

    /**
     * Reset dialogue back to the beginning if you ever want to replay it.
     */
    public void reset() {
        index = -1;
    }
}
