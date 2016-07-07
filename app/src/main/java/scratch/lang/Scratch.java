/**
 * Original JavaScript Version: http://scratch-lang.notimetoplay.org/scratch-lang4.js
 */
package scratch.lang;

import java.util.HashMap;
import java.util.Stack;

class ScratchLexer {
    private String text;
    private int position; // Beginning of TEXT.

    public ScratchLexer(String text) {
        this.text = text;
        position = 0;
    }

    // Trying to avoid regular expressions here.
    public boolean isWhitespace(char ch) {
        return ch == ' '
                || ch == '\t'
                || ch == '\r'
                || ch == '\n';
    }

    public String nextWord() {
        if (position >= text.length()) {
            return null;
        }
        while (isWhitespace(text.charAt(position))) {
            position++;
            if (position >= text.length()) {
                return null;
            }
        }
        int new_pos = position;
        while (!isWhitespace(text.charAt(new_pos))) {
            new_pos++;
            if (new_pos >= text.length()) {
                break;
            }
        }
        String collector = text.substring(position, new_pos);
        new_pos++;
        position = new_pos; // Skip the delimiter.
        return collector;
    }

    public String nextCharsUpTo(char ch) {
        if (position >= text.length()) {
            return null;
        }
        int new_pos = position;
        while (text.charAt(new_pos) != ch) {
            new_pos++;
            if (new_pos >= text.length()) {
                throw new RuntimeException("Unexpected end of input");
            }
        }
        String collector = text.substring(position, new_pos);
        new_pos++;
        position = new_pos; // Skip the delimiter.
        return collector;
    }
}

public class Scratch {
    private HashMap<String, Code> dictionary = new HashMap<>();
    private Stack<Object> data_stack = new Stack<>();
    private Stack<Object> compile_buffer = new Stack<>();
    public Stack<Object> stack = data_stack;
    private boolean immediate = false;
    public ScratchLexer lexer;
    public String latest;
    public int code_pointer;
    public boolean break_state;

    public Scratch() {
        Object[] words = {
                "PRINT", new CodePrint(),
                ".", new CodePrint(),
                "PSTACK", new CodePstack(),
                ".S", new CodePstack(),
                "+", new CodeAdd(),
                "-", new CodeSub(),
                "*", new CodeMul(),
                "/", new CodeDiv(),
                "%", new CodeMod(),
                "SQRT", new CodeSqrt(),
                "DUP", new CodeDup(),
                "DROP", new CodeDrop(),
                "SWAP", new CodeSwap(),
                "OVER", new CodeOver(),
                "ROT", new CodeRot(),
                "CLEAR", new CodeClear(),
                "VAR", new CodeVar(),
                "STORE", new CodeStore(),
                "!", new CodeStore(),
                "FETCH", new CodeFetch(),
                "@", new CodeFetch(),
                "CONST", new CodeConst(),
                "\"", new CodeString(),
                "/*", new CodeCComment(),
                "(", new CodeComment(),
                "//", new CodeCCComment(),
                "DEF", new CodeDef(),
                ":", new CodeDef(),
                "END", new CodeEnd(),
                ";", new CodeEnd(),
                "[", new CodeList(),
                "LENGTH", new CodeLength(),
                "ITEM", new CodeItem(),
                "RUN", new CodeRun(),
                "TIMES", new CodeTimes(),
                "IFTRUE", new CodeIfTrue(),
                "IFFALSE", new CodeIfFalse(),
                "WHILE", new CodeWhile(),
                "?CONTINUE", new CodeContinue(),
                "?BREAK", new CodeBreak(),
                "LOOP", new CodeLoop(),
                "TRUE", new CodeTrue(),
                "FALSE", new CodeFalse(),
                "AND", new CodeAnd(),
                "OR", new CodeOr(),
                "NOT", new CodeNot(),
                "<", new CodeLess(),
                "<=", new CodeLE(),
                "=", new CodeEqual(),
                ">=", new CodeGE(),
                ">", new CodeGreater(),
        };
        for (int i = 0; i < words.length; i += 2) {
            define((String)words[i], (Code)words[i + 1]);
        }
    }

    public void define(String word, Code code) {
        dictionary.put(word.toUpperCase(), code);
    }

    public void run(String text) {
        lexer = new ScratchLexer(text);
        String word;
        while ((word = lexer.nextWord()) != null) {
            Object obj = compile(word);
            if (immediate) {
                interpret(obj);
                immediate = false;
            } else if (isCompiling()) {
                stack.push(obj);
            } else {
                interpret(obj);
            }
        }
    }

    public Object compile(String word) {
        word = word.toUpperCase();
        if (dictionary.containsKey(word)) {
            immediate = dictionary.get(word).immediate;
            return dictionary.get(word);
        }
        try {
            return Double.parseDouble(word);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Unknown word: [" + word + "]");
        }
    }

    public void interpret(Object word) {
        if (word instanceof Code) {
            ((Code)word).call(this);
        } else {
            stack.push(word);
        }
    }

    public void startCompiling() {
        stack = compile_buffer;
    }

    public void stopCompiling() {
        stack = data_stack;
    }

    private boolean isCompiling() {
        return stack == compile_buffer;
    }

    public static void main(String[] args) {
        Scratch terp = new Scratch();
        String[] texts = {
                "1 2 + print",
                "3 4 - print",
                "5 6 * print",
                "7 8 / print",
                "9 sqrt print",
                "10 dup pstack clear",
                "11 drop pstack",
                "12 13 swap pstack clear",
                "14 15 over pstack clear",
                "16 17 18 rot pstack clear",
                "var a 19 a ! a @ print",
                "20 const b b print",
                "\" 21\" print",
                "22 /* comment */ print",
                "23 ( comment ) print",
                "24 // comment\n print",
                ": c 25 print ; c",
                "true [ 26 . ] iftrue",
                "false [ 26.1 . ] iffalse",
                "false [ 27 . ] iffalse",
                "true [ 27.1 . ] iffalse",
                "true true and [ 28 . ] iftrue",
                "false true and [ 28.1 . ] iftrue",
                "true false or [ 29 . ] iftrue",
                "false false or [ 29.1 . ] iftrue",
                "false not [ 30 . ] iftrue",
                "31 31 < [ 31 . ] iffalse",
                "32 32 <= [ 32 . ] iftrue",
                "33 33 = [ 33 . ] iftrue",
                "34 34 > [ 34 . ] iffalse",
                "35 35 >= [ 35 . ] iftrue",
                "[ 36 . ] 3 times",
                "var d 0 d ! [ d @ 3 < ] [ 37 . d @ 1 + d ! ] while",
                "var e 0 e ! [ e @ 3 >= ?break 38 . e @ 1 + e ! ] loop",
                "var f 0 f ! [ 39 . f @ 1 + f ! f @ 3 < ?continue true ?break ] loop",
                "40 40 % .",
                "pstack",
        };
        for (String text : texts) {
            terp.run(text);
        }
    }
}

abstract class Code {
    public boolean immediate;

    public abstract void call(Scratch terp);
}

abstract class CodeImmediate extends Code {
    public CodeImmediate() {
        immediate = true;
    }
}

// Print and discard top of stack.
class CodePrint extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object tos = terp.stack.pop();
        System.out.println(tos);
    }
}

// Print out the contents of the stack.
class CodePstack extends Code {
    @Override
    public void call(Scratch terp) {
        System.out.println(terp.stack);
    }
}

class CodeAdd extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double tos = (double)terp.stack.pop();
        double _2os = (double)terp.stack.pop();
        terp.stack.push(_2os + tos);
    }
}

class CodeSub extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double tos = (double)terp.stack.pop();
        double _2os = (double)terp.stack.pop();
        terp.stack.push(_2os - tos);
    }
}

class CodeMul extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double tos = (double)terp.stack.pop();
        double _2os = (double)terp.stack.pop();
        terp.stack.push(_2os * tos);
    }
}

class CodeDiv extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double tos = (double)terp.stack.pop();
        double _2os = (double)terp.stack.pop();
        terp.stack.push(_2os / tos);
    }
}

class CodeMod extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double tos = (double)terp.stack.pop();
        double _2os = (double)terp.stack.pop();
        terp.stack.push((double)((int)_2os % (int)tos));
    }
}

class CodeSqrt extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        double tos = (double)terp.stack.pop();
        terp.stack.push(Math.sqrt(tos));
    }
}

// Duplicate the top of stack (TOS).
class CodeDup extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object tos = terp.stack.pop();
        terp.stack.push(tos);
        terp.stack.push(tos);
    }
}

// Throw away the TOS -- the opposite of DUP.
class CodeDrop extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        terp.stack.pop();
    }
}

// Exchange positions of TOS and second item on stack (2OS).
class CodeSwap extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object tos = terp.stack.pop();
        Object _2os = terp.stack.pop();
        terp.stack.push(tos);
        terp.stack.push(_2os);
    }
}

// Copy 2OS on top of stack.
class CodeOver extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object tos = terp.stack.pop();
        Object _2os = terp.stack.pop();
        terp.stack.push(_2os);
        terp.stack.push(tos);
        terp.stack.push(_2os);
    }
}

// Bring the 3rd item on stack to the top.
class CodeRot extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 3) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object tos = terp.stack.pop();
        Object _2os = terp.stack.pop();
        Object _3os = terp.stack.pop();
        terp.stack.push(_2os);
        terp.stack.push(tos);
        terp.stack.push(_3os);
    }
}

class CodeClear extends Code {
    @Override
    public void call(Scratch terp) {
        terp.stack.clear();
    }
}

class CodeVarRef extends Code {
    public Object value;

    @Override
    public void call(Scratch terp) {
        terp.stack.push(this);
    }
}

// Read next word from input and make it a variable.
class CodeVar extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        String var_name = terp.lexer.nextWord();
        if (var_name == null) {
            throw new RuntimeException("Unexpected end of input");
        }
        terp.define(var_name, new CodeVarRef());
    }
}

// Store value of 2OS into variable given by TOS.
class CodeStore extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        CodeVarRef reference = (CodeVarRef)terp.stack.pop();
        reference.value = terp.stack.pop();
    }
}

// Replace reference to variable on TOS with its value.
class CodeFetch extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        CodeVarRef reference = (CodeVarRef)terp.stack.pop();
        terp.stack.push(reference.value);
    }
}

class CodeConstRef extends Code {
    private final Object value;

    public CodeConstRef(Object value) {
        this.value = value;
    }

    @Override
    public void call(Scratch terp) {
        terp.stack.push(value);
    }
}

// Read next word from input and make it a constant with TOS as value.
class CodeConst extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        String const_name = terp.lexer.nextWord();
        if (const_name == null) {
            throw new RuntimeException("Unexpected end of input");
        }
        Object const_value = terp.stack.pop();
        terp.define(const_name, new CodeConstRef(const_value));
    }
}

class CodeString extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        terp.stack.push(terp.lexer.nextCharsUpTo('"'));
    }
}

class CodeCComment extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        String next_word;
        do {
            next_word = terp.lexer.nextWord();
            if (next_word == null) {
                throw new RuntimeException("Unexpected end of input");
            }
        } while (!next_word.endsWith("*/"));
    }
}

class CodeComment extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        terp.lexer.nextCharsUpTo(')');
    }
}

class CodeCCComment extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        terp.lexer.nextCharsUpTo('\n');
    }
}

class CodeWordRef extends Code {
    public Stack code;

    public CodeWordRef(Stack code) {
        this.code = code;
    }

    @Override
    public void call(Scratch terp) {
        int old_pointer = terp.code_pointer;
        terp.code_pointer = 0;
        while (terp.code_pointer >= 0 && terp.code_pointer < code.size()) {
            terp.interpret(code.get(terp.code_pointer));
            terp.code_pointer++;
        }
        terp.code_pointer = old_pointer;
    }
}

class CodeDef extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        String new_word = terp.lexer.nextWord();
        if (new_word == null) {
            throw new RuntimeException("Unexpected end of input");
        }
        terp.latest = new_word;
        terp.startCompiling();
    }
}

class CodeEnd extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        Stack<Object> new_code = new Stack<>();
        new_code.addAll(terp.stack); // Clone compile_buffer.
        terp.stack.clear(); // Clear compile_buffer.
        terp.define(terp.latest, new CodeWordRef(new_code));
        terp.stopCompiling();
    }
}

class CodeList extends CodeImmediate {
    @Override
    public void call(Scratch terp) {
        Stack<Object> list = new Stack<>();
        Stack<Object> old_stack = terp.stack;
        terp.stack = list;
        do {
            String next_word = terp.lexer.nextWord();
            if (next_word == null) {
                throw new RuntimeException("Unexpected end of input");
            }
            if (next_word.equals("]")) {
                break;
            }
            Object obj = terp.compile(next_word);
            if ((obj instanceof Code) && ((Code)obj).immediate) {
                terp.interpret(obj);
            } else {
                terp.stack.push(obj);
            }
        } while (true);
        terp.stack = old_stack;
        terp.stack.push(list);
    }
}

class CodeLength extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object temp = terp.stack.pop();
        if (!(temp instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        terp.stack.push(((Stack)temp).size());
    }
}

class CodeItem extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double index = (double)terp.stack.pop();
        Object obj = terp.stack.pop();
        if (obj instanceof Stack) {
            terp.stack.push(((Stack)obj).get((int)index));
        } else {
            throw new RuntimeException("Object expected");
        }
    }
}

class CodeRun extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object temp = terp.stack.pop();
        if (!(temp instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        terp.interpret(new CodeWordRef((Stack)temp));
    }
}

class CodeTimes extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double count = (double)terp.stack.pop();
        Object code = terp.stack.pop();
        if (!(code instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        Code word = new CodeWordRef((Stack)code);
        for (int i = 0; i < count; i++) {
            word.call(terp);
        }
    }
}

class CodeIfTrue extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object code = terp.stack.pop();
        boolean cond = (boolean)terp.stack.pop();
        if (!(code instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        if (cond) {
            terp.interpret(new CodeWordRef((Stack)code));
        }
    }
}

class CodeIfFalse extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object code = terp.stack.pop();
        boolean cond = (boolean)terp.stack.pop();
        if (!(code instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        if (!cond) {
            terp.interpret(new CodeWordRef((Stack)code));
        }
    }
}

class CodeWhile extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object code = terp.stack.pop();
        Object cond = terp.stack.pop();
        if (!(code instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        if (!(cond instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        Code code_word = new CodeWordRef((Stack)code);
        Code cond_word = new CodeWordRef((Stack)cond);
        do {
            cond_word.call(terp);
            if (terp.stack.size() < 1) {
                throw new RuntimeException("Not enough items on stack");
            }
            boolean val = (boolean)terp.stack.pop();
            if (val) {
                break;
            }
            code_word.call(terp);
        } while (true);
    }
}

class CodeContinue extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        boolean cond = (boolean)terp.stack.pop();
        if (cond) {
            terp.code_pointer = -1;
        }
    }
}

class CodeBreak extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        boolean cond = (boolean)terp.stack.pop();
        if (cond) {
            terp.code_pointer = Integer.MIN_VALUE;
            terp.break_state = true;
        }
    }
}

class CodeLoop extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        Object code = terp.stack.pop();
        if (!(code instanceof Stack)) {
            throw new RuntimeException("List expected");
        }
        Code code_word = new CodeWordRef((Stack)code);
        boolean old_break_state = terp.break_state;
        terp.break_state = false;
        do {
            code_word.call(terp);
        } while (!terp.break_state);
        terp.break_state = old_break_state;
    }
}

class CodeTrue extends Code {
    @Override
    public void call(Scratch terp) {
        terp.stack.push(true);
    }
}

class CodeFalse extends Code {
    @Override
    public void call(Scratch terp) {
        terp.stack.push(false);
    }
}

class CodeAnd extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        boolean term2 = (boolean)terp.stack.pop();
        boolean term1 = (boolean)terp.stack.pop();
        terp.stack.push(term1 && term2);
    }
}

class CodeOr extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        boolean term2 = (boolean)terp.stack.pop();
        boolean term1 = (boolean)terp.stack.pop();
        terp.stack.push(term1 || term2);
    }
}

class CodeNot extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 1) {
            throw new RuntimeException("Not enough items on stack");
        }
        boolean term = (boolean)terp.stack.pop();
        terp.stack.push(!term);
    }
}

class CodeLess extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double term2 = (double)terp.stack.pop();
        double term1 = (double)terp.stack.pop();
        terp.stack.push(term1 < term2);
    }
}

class CodeLE extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double term2 = (double)terp.stack.pop();
        double term1 = (double)terp.stack.pop();
        terp.stack.push(term1 <= term2);
    }
}

class CodeEqual extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double term2 = (double)terp.stack.pop();
        double term1 = (double)terp.stack.pop();
        terp.stack.push(term1 == term2);
    }
}

class CodeGE extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double term2 = (double)terp.stack.pop();
        double term1 = (double)terp.stack.pop();
        terp.stack.push(term1 >= term2);
    }
}

class CodeGreater extends Code {
    @Override
    public void call(Scratch terp) {
        if (terp.stack.size() < 2) {
            throw new RuntimeException("Not enough items on stack");
        }
        double term2 = (double)terp.stack.pop();
        double term1 = (double)terp.stack.pop();
        terp.stack.push(term1 > term2);
    }
}
