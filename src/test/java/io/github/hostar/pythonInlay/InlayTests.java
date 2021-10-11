package test.java.io.github.hostar.pythonInlay;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.presentation.PresentationRenderer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Inlay;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.utils.inlays.InlayHintsProviderTestCase;
import io.github.hostar.pythonInlay.PythonParametersInlayProvider;

import java.util.regex.Pattern;

public class InlayTests extends InlayHintsProviderTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    private void testTypeHints(String text) {
        testProvider("main.py", text, new PythonParametersInlayProvider(), new NoSettings());
    }

    public void testSimpleFile() {
        String text = "def _internal(aa, bb):\n" +
                      "    return aa + bb\n" +
                      "\n" +
                      "\n" +
                      "def outer(qwe, asd, sdf, *args):\n" +
                      "    _internal(<# aa #>\"xx\", *args)\n" +
                      "    return qwe + asd + sdf\n" +
                      "\n" +
                      "\n" +
                      "def print_hi(name, surname):\n" +
                      "    print(f'Hi, {name}')\n" +
                      "\n" +
                      "\n" +
                      "if __name__ == '__main__':\n" +
                      "    print_hi(<# name #>'John', <# surname #>'Smith')\n" +
                      "    outer(<# qwe #>'aa', <# asd #>'bb', <# sdf #>11, 'sf')\n" +
                      "    outer(asd='aaa')\n";
        testTypeHints(text);
    }

    public void testDataClass() {
        String text = "from dataclasses import dataclass\n" +
                      "\n" +
                      "@dataclass\n" +
                      "class InventoryItem:\n" +
                      "    \"\"\"Class for keeping track of an item in inventory.\"\"\"\n" +
                      "    name: str\n" +
                      "    unit_price: float\n" +
                      "    quantity_on_hand: int = 0\n" +
                      "\n" +
                      "    def total_cost(self) -> float:\n" +
                      "        return self.unit_price * self.quantity_on_hand\n" +
                      "\n" +
                      "item = InventoryItem(<# name #>\"myName\",<# unit_price #>12.0,quantity_on_hand=5)";
        testTypeHints(text);
    }

    public void testDataClass2() {
        String text = "from dataclasses import dataclass\n" +
                      "\n" +
                      "@dataclass\n" +
                      "class InventoryItem:\n" +
                      "    \"\"\"Class for keeping track of an item in inventory.\"\"\"\n" +
                      "    name: str\n" +
                      "    unit_price: float\n" +
                      "    quantity_on_hand: int = 0\n" +
                      "\n" +
                      "    def total_cost(self) -> float:\n" +
                      "        return self.unit_price * self.quantity_on_hand\n" +
                      "\n" +
                      "item = InventoryItem(<# name #>\"myName\",quantity_on_hand=5)";
        testTypeHints(text);
    }
}
