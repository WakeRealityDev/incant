package com.yrek.incant.glk;

import android.util.Log;

import java.util.List;

import com.yrek.ifstd.glk.GlkEvent;

public class SpeechMunger {
    private static final String TAG = SpeechMunger.class.getSimpleName();

    public static String chooseInput(List<String> options) {
        String input = options.get(0);
        if ("south east".equals(input)) {
            input = "southeast";
        } else if ("go south east".equals(input)) {
            input = "go southeast";
        } else if ("no I".equals(input)) {
            input = "north";
        } else if ("why".equals(input)) {
            input = "west";
        } else if ("wat".equals(input)) {
            input = "wait";
        } else if (input.startsWith("exam in ")) {
            input = "examine" + input.substring(7);
        } else if ("Digg".equals(input)) {
            input = "dig";
        } else if (input.startsWith("Digg ")) {
            input = "dig" + input.substring(4);
        } else if (input.startsWith("where the ") || input.startsWith("where a ") || input.startsWith("where an ")) {
            input = "wear" + input.substring(5);
        } else if (input.startsWith("but ")) {
            input = "put" + input.substring(3);
        } else if (input.startsWith("we the ") || input.startsWith("we a ") || input.startsWith("we an ")) {
            input = "read" + input.substring(2);
        } else if (input.startsWith("going to ")) {
            input = "go in" + input.substring(6);
        } else if (input.startsWith("\n")) {
            input = "enter " + input.substring(1);
        } else if (input.startsWith(".")) {
            input = "point " + input.substring(1);
        }
        Log.d(TAG,"chooseInput:"+options+",result="+input);
        return input;
    }

    public static int chooseCharacterInput(List<String> options) {
        Log.d(TAG,"chooseCharacterInput:"+options);
        return chooseCharacterInput(options.get(0));
    }

    public static int chooseCharacterInput(String input) {
        if ("".equals(input)) {
            return '\n';
        } else if ("space".equals(input)) {
            return ' ';
        } else if ("enter".equals(input)) {
            return '\n';
        } else if ("left".equals(input)) {
            return GlkEvent.KeycodeLeft;
        } else if ("right".equals(input)) {
            return GlkEvent.KeycodeRight;
        } else if ("up".equals(input)) {
            return GlkEvent.KeycodeUp;
        } else if ("down".equals(input)) {
            return GlkEvent.KeycodeDown;
        } else if ("return".equals(input)) {
            return GlkEvent.KeycodeReturn;
        } else if ("delete".equals(input)) {
            return GlkEvent.KeycodeDelete;
        } else if ("escape".equals(input)) {
            return GlkEvent.KeycodeEscape;
        } else if ("tab".equals(input)) {
            return GlkEvent.KeycodeTab;
        } else if ("page up".equals(input)) {
            return GlkEvent.KeycodePageUp;
        } else if ("page down".equals(input)) {
            return GlkEvent.KeycodePageDown;
        } else if ("home".equals(input)) {
            return GlkEvent.KeycodeHome;
        } else if ("end".equals(input)) {
            return GlkEvent.KeycodeEnd;
        } else if ("f1".equals(input)) {
            return GlkEvent.KeycodeFunc1;
        } else if ("f2".equals(input)) {
            return GlkEvent.KeycodeFunc2;
        } else if ("f3".equals(input)) {
            return GlkEvent.KeycodeFunc3;
        } else if ("f4".equals(input)) {
            return GlkEvent.KeycodeFunc4;
        } else if ("f5".equals(input)) {
            return GlkEvent.KeycodeFunc5;
        } else if ("f6".equals(input)) {
            return GlkEvent.KeycodeFunc6;
        } else if ("f7".equals(input)) {
            return GlkEvent.KeycodeFunc7;
        } else if ("f8".equals(input)) {
            return GlkEvent.KeycodeFunc8;
        } else if ("f9".equals(input)) {
            return GlkEvent.KeycodeFunc9;
        } else if ("f10".equals(input)) {
            return GlkEvent.KeycodeFunc10;
        } else if ("f11".equals(input)) {
            return GlkEvent.KeycodeFunc11;
        } else if ("f12".equals(input)) {
            return GlkEvent.KeycodeFunc12;
        }
        return input.charAt(0);
    }

    public static StringBuilder fixOutput(StringBuilder output) {
        for (int i = 0; i < output.length(); i++) {
            switch (output.charAt(i)) {
            case '<': case '>': case '*':
                output.setCharAt(i, ' ');
                break;
            default:
            }
        }
        return output;
    }
}
