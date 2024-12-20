package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	private final Interpreter interpreter;
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();
	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;

	Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	private enum FunctionType {
		NONE, FUNCTION, METHOD
	}

	private enum ClassType {
		NONE, CLASS, SUBCLASS
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		this.declare(stmt.name);
		if (stmt.initializer != null)
			this.resolve(stmt.initializer);
		this.define(stmt.name);
		return null;
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		this.beginScope();
		this.resolve(stmt.statements);
		this.endScope();
		return null;
	}

	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		this.resolve(stmt.condition);
		this.resolve(stmt.thenBranch);
		if (stmt.elseBranch != null)
			this.resolve(stmt.elseBranch);
		return null;
	}

	@Override
	public Void visitWhileStmt(Stmt.While stmt) {
		this.resolve(stmt.condition);
		this.resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		this.declare(stmt.name);
		this.define(stmt.name);

		this.resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		var enclosingClass = this.currentClass;
		this.currentClass = ClassType.CLASS;

		this.declare(stmt.name);
		this.define(stmt.name);

		if (stmt.super_ != null) {
			if (stmt.super_.name.lexeme.equals(stmt.name.lexeme)) {
				Lox.error(stmt.super_.name, "A class can't inherit from itself.");
			}
			this.currentClass = ClassType.SUBCLASS;
			this.resolve(stmt.super_);
			beginScope();
			this.scopes.peek().put("super", true);
		}

		this.beginScope();
		this.scopes.peek().put("this", true);

		for (var method : stmt.methods) {
			this.resolveFunction(method, FunctionType.METHOD);
		}

		this.endScope();
		if (stmt.super_ != null)
			this.endScope();

		this.currentClass = enclosingClass;
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		this.resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		this.resolve(stmt.expression);
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		if (this.currentFunction == FunctionType.NONE)
			Lox.error(stmt.keyword, "Can't return from top level code.");

		if (stmt.value != null)
			this.resolve(stmt.value);

		return null;
	}

	@Override
	public Void visitVariableExpr(Expr.Variable expr) {
		if (!this.scopes.isEmpty() && Boolean.FALSE.equals(this.scopes.peek().get(expr.name.lexeme))) {
			Lox.error(expr.name, "Can't read local variable in it's own initializer");
		}

		this.resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitAssignExpr(Expr.Assign expr) {
		this.resolve(expr.value);
		this.resolveLocal(expr, expr.name);
		return null;
	}

	@Override
	public Void visitBinaryExpr(Expr.Binary expr) {
		this.resolve(expr.left);
		this.resolve(expr.right);
		return null;
	}

	@Override
	public Void visitLogicalExpr(Expr.Logical expr) {
		this.resolve(expr.left);
		this.resolve(expr.right);
		return null;
	}

	@Override
	public Void visitUnaryExpr(Expr.Unary expr) {
		this.resolve(expr.right);
		return null;
	}

	@Override
	public Void visitCallExpr(Expr.Call expr) {
		this.resolve(expr.callee);

		for (var arg : expr.arguments) {
			this.resolve(arg);
		}

		return null;
	}

	@Override
	public Void visitGetExpr(Expr.Get expr) {
		this.resolve(expr.object);
		return null;
	}

	@Override
	public Void visitSetExpr(Expr.Set expr) {
		this.define(expr.name);
		return null;
	}

	@Override
	public Void visitThisExpr(Expr.This expr) {
		if (this.currentClass != ClassType.CLASS && this.currentClass != ClassType.SUBCLASS) {
			Lox.error(expr.keyword, "Can't use 'this' outside a class");
		}
		this.resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitSuperExpr(Expr.Super expr) {
		if (this.currentClass != ClassType.SUBCLASS) {
			Lox.error(expr.keyword, "Can't use 'super' outside a class with superclass");
		}

		this.resolveLocal(expr, expr.keyword);
		return null;
	}

	@Override
	public Void visitGroupingExpr(Expr.Grouping expr) {
		this.resolve(expr.expression);
		return null;
	}

	@Override
	public Void visitLiteralExpr(Expr.Literal expr) {
		return null;
	}

	void resolve(List<Stmt> stmts) {
		for (var stmt : stmts) {
			this.resolve(stmt);
		}
	}

	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}

	private void resolve(Expr expr) {
		expr.accept(this);
	}

	private void beginScope() {
		this.scopes.push(new HashMap<>());
	}

	private void endScope() {
		this.scopes.pop();
	}

	private void declare(Token name) {
		if (this.scopes.isEmpty())
			return;

		var scope = this.scopes.peek();
		if (scope.containsKey(name.lexeme))
			Lox.error(name, "Already a variable with this name in this scope.");

		scope.put(name.lexeme, false);
	}

	private void define(Token name) {
		if (this.scopes.isEmpty())
			return;

		var scope = this.scopes.peek();
		scope.put(name.lexeme, true);
	}

	private void resolveLocal(Expr expr, Token name) {
		for (var i = this.scopes.size() - 1; i >= 0; i--) {
			if (this.scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, this.scopes.size() - 1 - i);
				return;
			}
		}
	}

	private void resolveFunction(Stmt.Function function, FunctionType type) {
		var enclosingFunction = this.currentFunction;
		this.currentFunction = type;

		this.beginScope();
		for (var param : function.params) {
			this.declare(param);
			this.define(param);
		}

		this.resolve(function.body);
		this.endScope();

		this.currentFunction = enclosingFunction;
	}
}