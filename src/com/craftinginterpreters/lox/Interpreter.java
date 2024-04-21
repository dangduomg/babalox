package com.craftinginterpreters.lox;

import java.lang.Math;

class Interpreter implements Expr.Visitor<Object> {
  void interpret(Expr expression) {
    try {
      Object value = this.evaluate(expression);
      System.out.println(this.stringify(value));
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
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
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = this.evaluate(expr.right);
    
    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        this.checkNumberOperand(expr.operator, right);
        return -(double)right;
    }
    
    // unreachable
    return null;
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
  
  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }
  
  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = this.evaluate(expr.left);
    Object right = this.evaluate(expr.right); 
    
    switch (expr.operator.type) {
      case EQUAL_EQUAL: return isEqual(left, right);
      case BANG_EQUAL: return !isEqual(left, right);
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