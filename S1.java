// Hand-written R1 compiler
import java.io.*;
import java.util.*;
//======================================================
class R1
{  
  public static void main(String[] args) throws 
                                             IOException
  {
    System.out.println("R1 compiler written by Christopher Brady");
    if (args.length != 1)
    {
      System.err.println("Wrong number cmd line args");  
      System.exit(1);
    }

    // set to true to debug token manager
    boolean debug = false;

    // build the input and output file names
    String inFileName = args[0] + ".s";
    String outFileName = args[0] + ".a";

    // construct file objects
    Scanner inFile = new Scanner(new File(inFileName));
    PrintWriter outFile = new PrintWriter(outFileName);

    // identify compiler/author in the output file
    outFile.println("; from R1 compiler written by Christopher Brady");
    outFile.println("          !register");

    // construct objects that make up compiler
    R1SymTab st = new R1SymTab();
    R1TokenMgr tm =  new R1TokenMgr(inFile, outFile, debug);
    R1CodeGen cg = new R1CodeGen(outFile, st);
    R1Parser parser = new R1Parser(st, tm, cg);

    // parse and translate
    try
    {
      parser.parse();
    }      
    catch (RuntimeException e) 
    {
      System.err.println(e.getMessage());
      outFile.println(e.getMessage());
      outFile.close();
      System.exit(1);
    }

    outFile.close();
  }
}                                           // end of S1
//======================================================
interface R1Constants
{
  // integers that identify token kinds
  int EOF = 0;
  int PRINTLN = 1;
  int UNSIGNED = 2;
  int ID = 3;
  int ASSIGN = 4;
  int SEMICOLON = 5;
  int LEFTPAREN = 6;
  int RIGHTPAREN = 7;
  int PLUS = 8;
  int MINUS = 9;
  int TIMES = 10;
  int ERROR = 11;

  // tokenImage provides string for each token kind
  String[] tokenImage = 
  {
    "<EOF>",
    "\"println\"",
    "<UNSIGNED>",
    "<ID>",
    "\"=\"",
    "\";\"",
    "\"(\"",
    "\")\"",
    "\"+\"",
    "\"-\"",
    "\"*\"",
    "<ERROR>"
  };
}                                  // end of S1Constants
//======================================================
class R1SymTab
{
  private ArrayList<String> symbol;
  private ArrayList<String> value;
  private ArrayList<Boolean> needsDw;
  //-----------------------------------------
  public R1SymTab()
  {
    symbol = new ArrayList<String>();
    value = new ArrayList<String>();
    needsDw = new ArrayList<Boolean>();
  }
  //-----------------------------------------
  public void enter(String s)
  {
    int index = symbol.indexOf(s);

    // if s is not in symbol, then add it 
    if (index < 0) 
      symbol.add(s);
  }
  public int enter(String s, String v, boolean b)//####################################################
  {
    
    int index = symbol.indexOf(s);
    
    // if s is not in symbol, then add it 
    if (index >= 0)
        return index;
    index = symbol.size();
    symbol.add(s);
    needsDw.add(b);
    value.add(v);
    
    return index;
  }
  //-----------------------------------------
  public String getSymbol(int index)
  {
    return symbol.get(index);
  }
  public String getValue(int index){
      return value.get(index);
  }
  public boolean getneedsDw(int index){
    return needsDw.get(index);
  }
  public void setneedsDw(int index){
    needsDw.set(index, true);
  }
  //-----------------------------------------
  public int getSize()
  {
    return symbol.size();
  }
  public boolean isConstant(int x){
    String s = getSymbol(x);
    if (s.charAt(0) != '@')
      return false;
    if (s.charAt(1) == '_')
      return true;
    try{
        Integer.parseInt(s.substring(1));
    } catch(NumberFormatException e){
        return false;
    } catch(NullPointerException e){
        return false;
    }
    return true;
  }
  public boolean isTemp(int index){
    String image = getSymbol(index);
    if (image.charAt(0) == '@' && image.charAt(1) == 't')
      return true;
    else
      return false;
  }
  public boolean isldcConstant(int opndIndex){
      if (isConstant(opndIndex)){
        int num = Integer.parseInt(getValue(opndIndex));
        if(num >= 0 && num <= 4095)
          return true;
      }
      return false;
  }
}                                     // end of R1SymTab
//======================================================
class R1TokenMgr implements R1Constants
{
  private Scanner inFile;          
  private PrintWriter outFile;
  private boolean debug;
  private char currentChar;
  private int currentColumnNumber;
  private int currentLineNumber;
  private String inputLine;    // holds 1 line of input
  private Token token;         // holds 1 token
  private StringBuffer buffer; // token image built here
  //-----------------------------------------
  public R1TokenMgr(Scanner inFile,  PrintWriter outFile, boolean debug)
  {
    this.inFile = inFile;
    this.outFile = outFile;
    this.debug = debug;
    currentChar = '\n';        //  '\n' triggers read
    currentLineNumber = 0;
    buffer = new StringBuffer();
  }
  //-----------------------------------------
  public Token getNextToken()
  {
    // skip whitespace
    while (Character.isWhitespace(currentChar))
      getNextChar();

    // construct token to be returned to parser
    token = new Token();   
    token.next = null;

    // save start-of-token position
    token.beginLine = currentLineNumber;
    token.beginColumn = currentColumnNumber;

    // check for EOF
    if (currentChar == EOF)
    {
      token.image = "<EOF>";
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;
      token.kind = EOF;
    }

    else  // check for unsigned int
    if (Character.isDigit(currentChar)) 
    {
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar); 
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();
      token.kind = UNSIGNED;
    }

    else  // check for identifier
    if (Character.isLetter(currentChar)) 
    { 
      buffer.setLength(0);  // clear buffer
      do  // build token image in buffer
      {
        buffer.append(currentChar);
        token.endLine = currentLineNumber;
        token.endColumn = currentColumnNumber;
        getNextChar();
      } while (Character.isLetterOrDigit(currentChar));
      // save buffer as String in token.image
      token.image = buffer.toString();

      // check if keyword
      if (token.image.equals("println"))
        token.kind = PRINTLN;
      else  // not a keyword so kind is ID
        token.kind = ID;
    }

    else  // process single-character token
    {  
      switch(currentChar)
      {
        case '=':
          token.kind = ASSIGN;
          break;                               
        case ';':
          token.kind = SEMICOLON;
          break;                               
        case '(':
          token.kind = LEFTPAREN;
          break;                               
        case ')':
          token.kind = RIGHTPAREN;
          break;                               
        case '+':
          token.kind = PLUS;
          break;                               
        case '-':
          token.kind = MINUS;
          break;                               
        case '*':
          token.kind = TIMES;
          break;                               
        default:
          token.kind = ERROR;
          break;                               
      }

      // save currentChar as String in token.image
      token.image = Character.toString(currentChar);

      // save end-of-token position
      token.endLine = currentLineNumber;
      token.endColumn = currentColumnNumber;

      getNextChar();  // read beyond end of token
    }

    // token trace appears as comments in output file
    if (debug)
      outFile.printf(
        "; kd=%3d bL=%3d bC=%3d eL=%3d eC=%3d im=%s%n",
        token.kind, token.beginLine, token.beginColumn, 
        token.endLine, token.endColumn, token.image);

    return token;     // return token to parser
  }     
  //-----------------------------------------
  private void getNextChar()
  {
    if (currentChar == EOF)
      return;

    if (currentChar == '\n')        // need next line?
    {
      if (inFile.hasNextLine())     // any lines left?
      {
        inputLine = inFile.nextLine();  // get next line
        // output source line as comment
        outFile.println("; " + inputLine);
        inputLine = inputLine + "\n";   // mark line end
        currentColumnNumber = 0;
        currentLineNumber++;   
      }                                
      else  // at end of file
      {
         currentChar = EOF;
         return;
      }
    }

    // get next char from inputLine
    currentChar = 
                inputLine.charAt(currentColumnNumber++);

    // in S2, test for single-line comment goes here
  }
}                                   // end of R1TokenMgr
//======================================================
class R1Parser implements R1Constants
{
  private R1SymTab st;
  private R1TokenMgr tm;
  private R1CodeGen cg;
  private Token currentToken;
  private Token previousToken; 
  //-----------------------------------------
  public R1Parser(R1SymTab st, R1TokenMgr tm, 
                                           R1CodeGen cg)
  {
    this.st = st;
    this.tm = tm;
    this.cg = cg;   
    // prime currentToken with first token
    currentToken = tm.getNextToken(); 
    previousToken = null;
  }
  //-----------------------------------------
  // Construct and return an exception that contains
  // a message consisting of the image of the current
  // token, its location, and the expected tokens.
  //
  private RuntimeException genEx(String errorMessage)
  {
    return new RuntimeException("Encountered \"" + 
      currentToken.image + "\" on line " + 
      currentToken.beginLine + ", column " + 
      currentToken.beginColumn + "." +
      System.getProperty("line.separator") + 
      errorMessage);
  }
  //-----------------------------------------
  // Advance currentToken to next token.
  //
  private void advance()
  {
    previousToken = currentToken; 

    // If next token is on token list, advance to it.
    if (currentToken.next != null)
      currentToken = currentToken.next;

    // Otherwise, get next token from token mgr and 
    // put it on the list.
    else
      currentToken = 
                  currentToken.next = tm.getNextToken();
  }
  //-----------------------------------------
  // getToken(i) returns ith token without advancing
  // in token stream.  getToken(0) returns 
  // previousToken.  getToken(1) returns currentToken.
  // getToken(2) returns next token, and so on.
  //
  private Token getToken(int i)
  {
    if (i <= 0)
      return previousToken;

    Token t = currentToken;
    for (int j = 1; j < i; j++)  // loop to ith token
    {
      // if next token is on token list, move t to it
      if (t.next != null)
        t = t.next;

      // Otherwise, get next token from token mgr and 
      // put it on the list.
      else
        t = t.next = tm.getNextToken();
    }
    return t;
  }
  //-----------------------------------------
  // If the kind of the current token matches the
  // expected kind, then consume advances to the next
  // token. Otherwise, it throws an exception.
  //
  private void consume(int expected)
  {
    if (currentToken.kind == expected)
      advance();
    else
      throw genEx("Expecting " + tokenImage[expected]);
  }
  //-----------------------------------------
  public void parse()
  {
    program();   // program is start symbol for grammar
  }
  //-----------------------------------------
  private void program()
  {
    statementList();
    cg.endCode();
    if (currentToken.kind != EOF)  //garbage at end?
      throw genEx("Expecting <EOF>");
  }
  //-----------------------------------------
  private void statementList()
  {
    switch(currentToken.kind)
    {
      case ID:
      case PRINTLN:
        statement();
        statementList();
        break;
      case EOF:
        ;
        break;
      default:
        throw genEx("Expecting statement or <EOF>");
    }
  }
  //-----------------------------------------
  private void statement()
  {
    switch(currentToken.kind)
    {
      case ID: 
        assignmentStatement(); 
        break;
      case PRINTLN:    
        printlnStatement(); 
        break;
      default:         
        throw genEx("Expecting statement");
    }
  }
  //-----------------------------------------
  private void assignmentStatement()
  {
    
    Token t;
    int left, expVal;
    t = currentToken;
    consume(ID);
    left = st.enter(t.image, "0", true); 
    consume(ASSIGN);
    expVal = expr();
    cg.assign(left, expVal);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private void printlnStatement()
  {
    int expVal;
    consume(PRINTLN);
    consume(LEFTPAREN);
    expVal = expr();
    cg.emitLoad(expVal);
    cg.emitInstruction("dout");
    cg.emitInstruction("ldc", "'\\n'");
    cg.emitInstruction("aout");
    consume(RIGHTPAREN);
    consume(SEMICOLON);
  }
  //-----------------------------------------
  private int expr()
  {
    int left, expVal;
    left = term();
    expVal = termList(left);
    return expVal;
  }
  //-----------------------------------------
  private int termList(int left)
  {
    int right, temp, expVal;
    switch(currentToken.kind)
    {
      case PLUS:
        consume(PLUS);
        right = term();
        if(st.isConstant(left) && st.isConstant(right)){
            int result = Integer.parseInt(st.getValue(left))+Integer.parseInt(st.getValue(right));
            int index;
            if(result >= 0)
                temp = st.enter("@"+result, result+"", false);
            else
                temp = st.enter("@_"+-result, result+"", false);
        }
        else
            temp = cg.add(left, right);
        expVal = termList(temp);
        return expVal;
      case RIGHTPAREN:
      case SEMICOLON:
        return left;
      default:
        throw genEx("Expecting \"+\", \")\", or \";\"");
    }
  }
  //-----------------------------------------
  private int term()
  {
    int left, termVal;
    left = factor();
    termVal = factorList(left);
    return termVal;
  }
  //-----------------------------------------
  private int factorList(int left)
  {
    int right, temp, termVal;
    switch(currentToken.kind)
    {
      case TIMES:
        consume(TIMES);
        right = factor();
        temp = cg.mult(left, right);
        termVal = factorList(temp);
        return termVal;
      case PLUS:
      case RIGHTPAREN:
      case SEMICOLON:
        return left;
      default:
        throw genEx("Expecting op, \")\", or \";\"");
    }
  }
  //-----------------------------------------
  private int factor()
  {  
    Token t;
    int index;
    switch(currentToken.kind)
    {
      case UNSIGNED:
        t = currentToken;
        consume(UNSIGNED);
        index = st.enter("@"+t.image, t.image, false);
        return index;
      case PLUS:
        consume(PLUS);
        t = currentToken;
        consume(UNSIGNED);
        index = st.enter("@"+t.image, t.image, false);
        return index;
      case MINUS:
        consume(MINUS);
        t = currentToken;
        consume(UNSIGNED);
        index = st.enter("@_"+t.image, "-"+t.image, false);
        return index;
      case ID:
        t = currentToken;
        consume(ID);
        index = st.enter(t.image, "0", true);
        return index;
      case LEFTPAREN:
        consume(LEFTPAREN);
        index = expr();
        consume(RIGHTPAREN);
        return index;
      default:
        throw genEx("Expecting factor");
    }
  }
}                                     // end of R1Parser
//======================================================
class R1CodeGen
{
  private PrintWriter outFile;
  private R1SymTab st;
  private int tempIndex;
  private int ac;
  //-----------------------------------------
  public R1CodeGen(PrintWriter outFile, R1SymTab st)
  {
    this.outFile = outFile;
    this.st = st;
    tempIndex = 0;
  }
  //-----------------------------------------
  public void emitInstruction(String op)
  {
    outFile.printf("          %-4s%n", op); 
  }
  //-----------------------------------------
  public void emitInstruction(String op, String opnd) 
  {           
    outFile.printf("          %-4s      %s%n", op,opnd); 
  }
  //-----------------------------------------
  private void emitdw(String label, String value)
  {           
    outFile.printf(
             "%-9s dw        %s%n", label + ":", value);
  }
  //-----------------------------------------
  public void endCode()
  {
    outFile.println();
    emitInstruction("halt");

    int size = st.getSize();
    // emit dw for each symbol in the symbol table
    for (int i=0; i < size; i++) 
      if(st.getneedsDw(i) == true)
        emitdw(st.getSymbol(i), st.getValue(i));
  }
  public void freeTemp(int opndIndex){
      if (st.isTemp(opndIndex))
        tempIndex--;
  }
  public void assign(int left, int expVal){ 
        if(ac != expVal)
          emitLoad(expVal);
        freeTemp(expVal);
        emitInstruction("st", st.getSymbol(left));
        st.setneedsDw(left);
        ac = left;
  }
  public int add(int left, int right){
      if(ac == left){
        emitInstruction("add", st.getSymbol(right));
      }
      else if(ac == right){
        emitInstruction("add", st.getSymbol(left));
      }
      else{
        if(st.isTemp(ac)){
          emitInstruction("st", st.getSymbol(ac));
          st.setneedsDw(ac);
        }
        emitLoad(left);
        emitInstruction("add", st.getSymbol(right));
      }
      freeTemp(left);
      freeTemp(right);
      int temp = getTemp();
      emitInstruction("st", st.getSymbol(temp));
      st.setneedsDw(left);
      st.setneedsDw(right);
      st.setneedsDw(temp);
      ac = temp;
      return temp;
  }
  public int mult(int left, int right){
      if(ac == left){
        emitInstruction("mult", st.getSymbol(right));
      }
      else if(ac == right){
        emitInstruction("mult", st.getSymbol(left));
      }
      else{
        emitLoad(left);
        emitInstruction("mult", st.getSymbol(right));
      }
      freeTemp(left);
      freeTemp(right);
      int temp = getTemp();
      emitInstruction("st", st.getSymbol(temp));
      st.setneedsDw(left);
      st.setneedsDw(right);
      st.setneedsDw(temp);
      ac = temp;
      return temp;
  }
  public void emitLoad(int opndIndex){
      if(st.isldcConstant(opndIndex))
        emitInstruction("ldc", st.getValue(opndIndex));
      else{
        emitInstruction("ld", st.getSymbol(opndIndex));
        st.setneedsDw(opndIndex);
      }
  }
  public int getTemp(){
      int index = st.enter("@t"+tempIndex++, "0", true);
      return index;
  }
}                                    // end of R1CodeGen
