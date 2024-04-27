package com.craftinginterpreters.lox;

import java.lang.Math;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  private Environment environment = new Environment();
  
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        this.execute(statement);
      }
    } catch (RuntimeError e) {
      Lox.runtimeError(e);
    }
  }
  
  private void execute(Stmt statement) {
    statement.accept(this);
  }
  
  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    this.evaluate(stmt.expression);
    return null;
  }
  
  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = this.evaluate(stmt.expression);
    System.out.println(this.stringify(value));
    return null;
  }
  
  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = this.evaluate(stmt.initializer);
    }
    
    this.environment.define(stmt.name, value);
    return null;
  }
  
  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    this.executeBlock(stmt.statements, new Environment(this.environment));
    return null;
  }
  
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      
      for (Stmt statement : statements) {
        this.execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }
  
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (this.isTruthy(this.evaluate(stmt.condition)))
      this.execute(stmt.thenBranch);
    else if (stmt.elseBranch != null)
      this.execute(stmt.elseBranch);
    
    return null;
  }
  
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (this.isTruthy(this.evaluate(stmt.condition)))
      this.execute(stmt.body);
    
    return null;
  }
  
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = this.evaluate(expr.value);
    this.environment.assign(expr.name, value);
    return value;
  }
  
  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }
  
  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return this.evaluate(expr.expression);
  }
  
  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    return this.environment.get(expr.name);
  }
  
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = this.evaluate(expr.right);
    
    switch (expr.operator.type) {
      case BANG:
        return !this.isTruthy(right);
      case MINUS:
        this.checkNumberOperand(expr.operator, right);
        return -(double)right;
    }
    
    // unreachable
    return null;
  }
  
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = this.evaluate(expr.left);
    Object right = this.evaluate(expr.right); 
    
    switch (expr.operator.type) {
      case EQUAL_EQUAL: return this.isEqual(left, right);
      case BANG_EQUAL: return !this.isEqual(left, right);
      case GREATER:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double)right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }
        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        throw new RuntimeError(expr.operator, "All operands must be either numbers or strings.");
      case MINUS:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case SLASH:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
      case STAR:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
      case STAR_STAR:
        this.checkNumberOperands(expr.operator, left, right);
        return Math.pow((double)left, (double)right);
      case PERCENT:
        this.checkNumberOperands(expr.operator, left, right);
        return (double)left % (double)right;
    }
    
    // unreachable
    return null;
  }
  
  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = this.evaluate(expr.left);
    
    if (expr.operator.type == TokenType.OR)
      if (this.isTruthy(left)) return left;
    else if (expr.operator.type == TokenType.AND)
      if (!this.isTruthy(left)) return left;
    
    return this.evaluate(expr.right);
  }
                      
  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }
  
  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }
  
  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }
  
  private boolean isEqual(Object left, Object right) {
    if (left == null && right == null) return true;
    if (left == null) return false;
    
    return left.equals(right);
  }
  
  private String stringify(Object object) {
    if (object == null) return "nil";
    
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    
    return object.toString();
  }
}