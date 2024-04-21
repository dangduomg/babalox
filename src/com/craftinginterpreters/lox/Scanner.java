package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.craftinginterpreters.lox.TokenType;

class Scanner {
  private static final Map<String, TokenType> keywords;
  static {
    keywords = new HashMap<>();
    keywords.put("and", TokenType.AND);
    keywords.put("class", TokenType.CLASS);
    keywords.put("else", TokenType.ELSE);
    keywords.put("false", TokenType.FALSE);
    keywords.put("for", TokenType.FOR);
    keywords.put("fun", TokenType.FUN);
    keywords.put("if", TokenType.IF);
    keywords.put("nil", TokenType.NIL);
    keywords.put("or", TokenType.OR);
    keywords.put("print", TokenType.PRINT);
    keywords.put("return", TokenType.RETURN);
    keywords.put("super", TokenType.SUPER);
    keywords.put("this", TokenType.THIS);
    keywords.put("true", TokenType.TRUE);
    keywords.put("var", TokenType.VAR);
    keywords.put("while", TokenType.WHILE);
  }
  
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  
  Scanner(String source) {
    this.source = source;
  }
  
  List<Token> scanTokens() {
    while (!this.isAtEnd()) {
      this.start = this.current;
      this.scanToken();
    }
    
    this.tokens.add(new Token(TokenType.EOF, "", null, this.line));
    return this.tokens;
  }
  
  private void scanToken() {
    char c = this.advance();
    switch (c) {
      case '(': this.addToken(TokenType.LEFT_PAREN); break;
      case ')': this.addToken(TokenType.RIGHT_PAREN); break;
      case '{': this.addToken(TokenType.LEFT_BRACE); break;
      case '}': this.addToken(TokenType.RIGHT_BRACE); break;
      case ',': this.addToken(TokenType.COMMA); break;
      case '.': this.addToken(TokenType.DOT); break;
      case '-': this.addToken(TokenType.MINUS); break;
      case '+': this.addToken(TokenType.PLUS); break;
      case ';': this.addToken(TokenType.SEMICOLON); break;
      case '%': this.addToken(TokenType.PERCENT); break;
      
      case '!':
        this.addToken(this.match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
        break;
      case '=':
        this.addToken(this.match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
        break;
      case '<':
        this.addToken(this.match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
        break;
      case '>':
        this.addToken(this.match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
        break;
      case '*':
        if (this.match('*')) {
          this.addToken(TokenType.STAR_STAR);
        } else {
          this.addToken(TokenType.STAR);
        }
        break;
      case '/':
        if (this.match('/')) {
          // line comment
          while (this.peek() != '\n' && !this.isAtEnd()) this.advance();
        } else {
          this.addToken(TokenType.SLASH);
        }
        break;
      
      case ' ':
      case '\t':
      case '\r':
        // ignore whitespace
        break;
        
      case '\n': this.line++; break;
      
      case '"': this.string('"'); break;
      case '\'': this.string('\''); break;
      
      default:
        if (this.isDigit(c)) {
          this.number();
        } else if (this.isAlpha(c)) {
          this.identifier();
        } else {
          Lox.error(this.line, "Unexpected character.");
        }
        break;
    }
  }
  
  private void identifier() {
    while (this.isAlphaNumeric(this.peek())) this.advance();
    String text = this.source.substring(this.start, this.current);
    TokenType type = keywords.get(text);
    if (type == null) type = TokenType.IDENTIFIER;
    this.addToken(type);
  }
  
  private void number() {
    while (this.isDigit(this.peek())) this.advance();
    
    // look for decimal part
    if (this.peek() == '.' && this.isDigit(this.peekNext())) {
      // consume '.'
      this.advance();
      
      while (this.isDigit(this.peek())) this.advance();
    }
    
    double value = Double.parseDouble(this.source.substring(this.start, this.current));
    this.addToken(TokenType.NUMBER, value);
  }
  
  private void string(char quote) {
    while (this.peek() != quote && !this.isAtEnd()) {
      if (this.peek() == '\n') this.line++;
      this.advance();
    }
    
    if (this.isAtEnd()) {
      Lox.error(this.line, "Unterminated string.");
      return;
    }
    
    // the closing quote
    this.advance();
    
    // trim the surrounding quotes
    String value = this.source.substring(this.start + 1, this.current - 1);
    this.addToken(TokenType.STRING, value);
  }
  
  private boolean match(char expected) {
    if (this.isAtEnd()) return false;
    if (this.source.charAt(this.current) != expected) return false;
    this.current++;
    return true;
  }
  
  private char peek() {
    if (this.isAtEnd()) return '\0';
    return this.source.charAt(this.current);
  }
  
  private char peekNext() {
    if (this.current + 1 >= this.source.length()) return '\0';
    return this.source.charAt(this.current + 1);
  }
  
  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }
  
  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
      (c >= 'A' && c <= 'Z') ||
      c == '_';
  }
  
  private boolean isAlphaNumeric(char c) {
    return this.isAlpha(c) || this.isDigit(c);
  }
  
  private boolean isAtEnd() {
    return this.current >= this.source.length();
  }
  
  private char advance() {
    return this.source.charAt(this.current++);
  }
  
  private void addToken(TokenType type) {
    this.addToken(type, null);
  }
  
  private void addToken(TokenType type, Object literal) {
    String text = this.source.substring(this.start, this.current);
    this.tokens.add(new Token(type, text, literal, this.line));
  }
}