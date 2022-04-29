# S2.py compiler 
import sys, time 

class Token:
   def __init__(self, line, column, kind, image):
      self.line = line         # source program line number of the token
      self.column = column     # source program column in which token starts
      self.kind = kind         # kind of the token
      self.image = image       # token in string form

# global variables 
oufile = None      # output (i.e., asm lang program) file
source = ''        # receives entire source program
sourceindex = 0    # index into the source code in source
line = 0           # current line number 
column = 0         # current column number
tokens = []        # list of tokens produced by tokenizer
tokenindex = -1    # index into tokens list
token = None       # current token
prevchar = '\n'    # '\n' in prevchar signals start of new line
symbol = []        # list of variable names

# constants
EOF           = 0      # end of file
PRINTLN       = 1      # 'print' keyword
UNSIGNED      = 2      # unsigned integer
ID            = 3      # identifier that is not a keyword
ASSIGN        = 4      # '=' assignment operator
SEMICOLON     = 5      # ';'
LEFTPAREN     = 6      # '('
RIGHTPAREN    = 7      # ')'
PLUS          = 8      # '+'
MINUS         = 9      # '-'
TIMES         = 10     # '*'
ERROR         = 11     # if not any of the above, then error
DIVIDE        = 12
LEFTBRACE     = 13
RIGHTBRACE    = 14
PRINT         = 15

# displayable names for each token kind
catnames = ['EOF', 'PRINTLN', 'UNSIGNED', 'ID', 'ASSIGN',
            'LEFTPAREN', 'RIGHTPAREN', 'PLUS', 'MINUS',
            'TIMES', 'ERROR', 'DIVIDE', 'LEFTBRACE', 'RIGHTBRACE', 'PRINT']

# keywords and their token categories}
keywords = {'println': PRINTLN, 'print': PRINT}

# one-character tokens and their token categories
smalltokens = {'=':ASSIGN, '(':LEFTPAREN, ')':RIGHTPAREN,
               '+':PLUS, '-':MINUS, '*':TIMES, ';':SEMICOLON, '':EOF, '/':DIVIDE, '{':LEFTBRACE, '}':RIGHTBRACE}

#################
# main function #
#################
def main():
   global source, tokens, token, outfile, lines

   if len(sys.argv) == 2:
      try:
         infile = open(sys.argv[1]+'.s', 'r')
         source = infile.read()   # read source code
      except IOError:
         print('Cannot read input file ' + sys.argv[1] + '.s')
         sys.exit(1)

      try:
         outfile = open(sys.argv[1] + '.a', 'w')
      except IOError:
         print('Cannot write to output file ' + sys.argv[1] + '.a')
         sys.exit(1)
         
   else:
      print('Wrong number of command line arguments')
      print('Format: python S2.py <infilebasename>')
      sys.exit(1)

   lines = source.splitlines()

   outfile.write('; ' + time.strftime('%c') + '%34s' % 'Christopher Brady\n')
   outfile.write('; ' + 'Compiler    = ' + sys.argv[0] + '\n')
   outfile.write('; ' + 'Input file  = ' + sys.argv[1] + '.s' + '\n')
   outfile.write('; ' + 'Output file = ' + sys.argv[1] + '.a' + '\n')

   try:
      tokenizer()             
      # parse and and generate assembly language
      outfile.write(
         ';------------------------------------------- Assembler code\n')
      parser()
 
   # on an error, display an error message
   # token is the token object on which the error was detected
   except RuntimeError as emsg: 
     # output slash n in place of newline
     image = token.image.replace('\n', '\\n')
     print('\nError on '+ "'" + image + "'" + ' line ' +
        str(token.line) + ' column ' + str(token.column))
     print(lines[token.line])
     print(emsg)         # message from RuntimeError object
     outfile.write('\nError on '+ "'" + image + "'" + ' line ' +
        str(token.line) + ' column ' + str(token.column) + '\n')
     outfile.write(str(emsg) + '\n') # message from RuntimeError object

   outfile.close()

####################
# tokenizer        #
####################
def tokenizer():
   global token
   curchar = ' '                       # prime curchar with space

   while True:
      # skip whitespace but not newlines
      while curchar.isspace():
         curchar = getchar() # get next char from source program

      # construct and initialize token
      token = Token(line, column, None, '')  

      if curchar.isdigit():            # start of unsigned int?
         token.kind = UNSIGNED         # save kind of token
         while True:
            token.image += curchar     # append curchar to image
            curchar = getchar()        # get next character
            if not curchar.isdigit():  # break if not a digit
               break

      elif curchar.isalpha() or curchar == '_':   # start of name?
         while True:
            token.image += curchar    # append curchar to image
            curchar = getchar()        # get next character
            # break if not letter, '_', or digit
            if not (curchar.isalnum() or curchar == '_'):
               break

         # determine if image is a keyword or name of variable
         if token.image in keywords:
            token.kind = keywords[token.image]
         else:
            token.kind = ID

      elif curchar in smalltokens:
         token.kind = smalltokens[curchar]      # get kind
         token.image = curchar
         curchar = getchar()      # move to first char after the token

      else:                         
         token.kind = ERROR    # invalid token 
         token.image = curchar     # save image
         raise RuntimeError('Invalid token')
      
      tokens.append(token)          # append token to tokens list
      if token.kind == EOF:     # finished tokenizing?
         break

# getchar() gets next char from source and adjusts line and column
def getchar():
   global sourceindex, column, line, prevchar

   # check if starting a new line
   if prevchar == '\n':    # '\n' signals start of a new line
      line += 1            # increment line number                             
      column = 0           # reset column number

   if sourceindex >= len(source): # at end of source code?
      column = 1                  # set EOF column to 1
      prevchar = ''
      return ''                   # null char signals end of source

   if source[sourceindex] ==  '/' and source[sourceindex+1] == '/':
      while source[sourceindex] != '\n':
         sourceindex += 1
         column += 1

   c = source[sourceindex] # get next char in the source program
   sourceindex += 1        # increment sourceindex to next character
   column += 1             # increment column number
   prevchar = c            # save current character

   return c                # return character to tokenizer()

####################
# Symbol table     #
####################
def enter(s):
   if s not in symbol:
      symbol.append(s)

####################
# parser functions #
####################
def advance():
   global token, tokenindex 
   tokenindex += 1
   if tokenindex >= len(tokens):
      raise RuntimeError('Unexpected end of file')
   token = tokens[tokenindex]

# advances if current token is the expected token
def consume(expectedcat):
   if (token.kind == expectedcat):
      advance()
   else:
     raise RuntimeError('Expecting ' + catnames[expectedcat])

def parser():
   advance()     # advance to first token
   program()     # generates assembly code for program
   if token.kind != EOF: # garbage at end?
      raise RuntimeError('Expecting end of file')

def program():
   statementList()
   endCode()

def statementList():
   if token.kind in [ID, PRINTLN, PRINT, SEMICOLON, LEFTBRACE]:
      statement()
      statementList()
   elif token.kind in [EOF, RIGHTBRACE]:
      pass
   else:
      raise RuntimeError('Expecting statement or EOF')

def statement():
   outfile.write('\n; ' + lines[token.line - 1] + '\n')
   if token.kind == ID:
      assignmentStatement()
   elif token.kind == PRINTLN:
      printlnStatement() 
   elif token.kind == PRINT:
      printStatement()
   elif token.kind == SEMICOLON:
      nullStatement()
   elif token.kind == LEFTBRACE:
      compoundStatement()

def assignmentStatement():
    enter(token.image)
    outfile.write('          pc   ' + token.image + '\n')                        
    advance()
    consume(ASSIGN)
    expr()
    outfile.write('          stav\n')
    consume(SEMICOLON)

def printlnStatement():
    consume(PRINTLN)
    consume(LEFTPAREN)
    expr()
    outfile.write('          dout\n')
    outfile.write("          pc   '\\n'" + '\n')
    outfile.write('          aout\n')
    consume(RIGHTPAREN)
    consume(SEMICOLON)

def printStatement(): 
    consume(PRINT)
    consume(LEFTPAREN)
    expr()
    outfile.write('          dout\n')
    consume(RIGHTPAREN)
    consume(SEMICOLON)

def nullStatement():
   consume(SEMICOLON)

def compoundStatement():
   consume(LEFTBRACE)
   statementList()
   consume(RIGHTBRACE)

def expr():
   term()
   termList()

def termList():
   if token.kind == PLUS:
      advance()
      term()
      outfile.write('          add\n')
      termList()
   elif token.kind == MINUS: 
      advance()
      term()
      outfile.write('          sub\n')
   elif token.kind in [RIGHTPAREN, SEMICOLON]:
      pass
   else:
      raise RuntimeError('Expecting "+", "-", ")", or ";"')
def term():
   factor()
   factorList()

def factorList():
   if token.kind == TIMES:
      advance()
      factor()
      outfile.write('          mult\n')
      factorList()
   elif token.kind == DIVIDE: 
      advance()
      factor()
      outfile.write('          div\n')
      factorList()
   elif token.kind in [PLUS, MINUS, RIGHTPAREN, SEMICOLON]:
      pass
   else:
      raise RuntimeError('Expecting op, ")", or ";"')
   
def factor():
   if token.kind == UNSIGNED:
      outfile.write('          pwc  ' + token.image + '\n')
      advance()
   elif token.kind == PLUS:
      advance()
      outfile.write('          pwc  ' + token.image + '\n')
      consume(UNSIGNED)
   elif token.kind == MINUS:
      advance()
      outfile.write('          pwc  ' + '-' + token.image + '\n')
      consume(UNSIGNED)
   elif token.kind == ID:
      enter(token.image)
      outfile.write('          p    ' + token.image + '\n')
      advance()
   elif token.kind == LEFTPAREN:
      advance()
      expr()
      consume(RIGHTPAREN)
   else:
      raise RuntimeError('Expecting factor')

############################
# code generator functions #
############################

def endCode():
   outfile.write('\n')
   outfile.write('          halt\n')

   for i in symbol:
     outfile.write(('%-10s:' % i) + 'dw ' + '0\n')

####################
# start of program #
####################
main()