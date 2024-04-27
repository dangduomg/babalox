package com.craftinginterpreters.lox;

import java.util.List;
import java.util.ArrayList;

class Parser {
  private final List<Token> tokens;
  private int current = 0;
  
  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }
  
  List<Stmt> parse() {
    List<Stmt> statements = new ArrayList<>();
    while (!this.isAtEnd()) {
      statements.add(this.declaration());
    }
    return statements;
  }
  
  private Stmt declaration() {
    try {
      if (this.match(TokenType.VAR)) return this.varDeclaration();
      
      return this.statement();
    } catch (ParseError e) {
      this.synchronize();
      return null;
    }
  }
  
  private Stmt varDeclaration() {
    Token name = this.consume(TokenType.IDENTIFIER, "Expect variable name.");
    
    Expr initializer = null;
    if (this.match(TokenType.EQUAL))
      initializer = this.expression();
    
    this.consume(TokenType.SEMICOLON, "Expect ';' after declaration");
    return new Stmt.Var(name, initializer);
  }
  
  private Stmt statement() {
    if (this.match(TokenType.LEFT_BRACE))
      return new Stmt.Block(this.block());
    if (this.match(TokenType.PRINT))
      return this.printStatement();
    if (this.match(TokenType.IF))
      return this.ifStatement();
    if (this.match(TokenType.WHILE))
      return this.whileStatement();
    return this.expressionStatement();
  }
  
  private Stmt printStatement() {
    Expr value = this.expression();
    this.consume(TokenType.SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }
  
  private Stmt expressionStatement() {
    Expr expr = this.expression();
    this.consume(TokenType.SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }
  
  private Stmt ifStatement() {
    this.consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = this.expression();
    this.consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
    
    Stmt thenBranch = this.statement();
    
    Stmt elseBranch = null;
    if (this.match(TokenType.ELSE))
      elseBranch = this.statement();
    
    return new Stmt.If(condition, thenBranch, elseBranch);
  }
  
  private Stmt whileStatement() {
    this.consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = this.expression();
    this.consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
    
    Stmt body = this.statement();
    return new Stmt.While(condition, body);
  }
  
  private List<Stmt> block() {
    List<Stmt> statements = new ArrayList<>();
    
    while (!this.check(TokenType.RIGHT_BRACE) && !this.isAtEnd()) {
      statements.add(this.declaration());
    }
    
    this.consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");    
    return statements;
  }
  
  private Expr expression() {
    return this.assignment();
  }
  
  private Expr assignment() {
    Expr expr = this.or();
    
    if (this.match(TokenType.EQUAL)) {
      Token equals = this.previous();
      Expr value = this.assignment();
      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
      }
      this.error(equals, "Invalid assignment target");
    }
    
    return expr;
  }
  
  private Expr or() {
    Expr expr = this.and();
    
    while (this.match(TokenType.OR)) {
      Token operator = this.previous();
      Expr right = this.and();
      expr = new Expr.Logical(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr and() {
    Expr expr = this.equality();
    
    while (this.match(TokenType.AND)) {
      Token operator = this.previous();
      Expr right = this.equality();
      expr = new Expr.Logical(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr equality() {
    Expr expr = this.comparison();
    
    while (this.match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = this.previous();
      Expr right = this.comparison();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr comparison() {
    Expr expr = this.term();
    
    while (this.match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      Token operator = this.previous();
      Expr right = this.term();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr term() {
    Expr expr = this.factor();
    
    while (this.match(TokenType.PLUS, TokenType.MINUS)) {
      Token operator = this.previous();
      Expr right = this.factor();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr factor() {
    Expr expr = this.unary();
    
    while (this.match(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
      Token operator = this.previous();
      Expr right = this.unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr unary() {
    if (this.match(TokenType.BANG, TokenType.MINUS)) {
      Token operator = this.previous();
      Expr right = this.unary();
      return new Expr.Unary(operator, right);
    }
    
    return this.power();
  }
  
  private Expr power() {
    Expr expr = this.primary();
    
    if (this.match(TokenType.STAR_STAR)) {
      Token operator = this.previous();
      Expr right = this.unary();
      expr = new Expr.Binary(expr, operator, right);
    }
    
    return expr;
  }
  
  private Expr primary() {
    if (this.match(TokenType.TRUE)) return new Expr.Literal(true);
    if (this.match(TokenType.FALSE)) return new Expr.Literal(false);
    if (this.match(TokenType.NIL)) return new Expr.Literal(null);
    
    if (this.match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);
    
    if (this.match(TokenType.IDENTIFIER)) return new Expr.Variable(this.previous());
    
    if (this.match(TokenType.LEFT_PAREN)) {
      Expr expr = this.expression();
      this.consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }
    
    throw this.error(this.peek(), "Expect expression.");
  }
  
  private boolean match(TokenType ...types) {
    for (TokenType type : types) {
      if (this.check(type)) {
        this.advance();
        return true;
      }
    }
    return false;
  }
  
  private Token consume(TokenType type, String message) {
    if (this.check(type)) return this.advance();
    
    throw this.error(this.peek(), message);
  }
  
  private boolean check(TokenType type) {
    if (this.isAtEnd()) return false;
    return this.peek().type == type;
  }
  
  private Token advance() {
    if (!this.isAtEnd()) this.current++;
    return this.previous();
  }
  
  private boolean isAtEnd() {
    return this.peek().type == TokenType.EOF;
  }
  
  private Token peek() {
    return this.tokens.get(this.current);
  }
  
  private Token previous() {
    return this.tokens.get(this.current - 1);
  }
  
  private ParseError error(Token token,  String message) {
    Lox.error(token, message);
    return new ParseError();
  }
  
  private void synchronize() {
    this.advance();
    
    while (!this.isAtEnd()) {
      if (this.previous().type == TokenType.SEMICOLON) return;
      
      switch (this.peek().type) {
        case CLASS:
        case FUN:
        case VAR:
        case FOR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }
      
      this.advance();
    }
  }
  
  private static class ParseError extends RuntimeException {}
}