package com.craftinginterpreters.lox;

import java.lang.Math;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
	private final Map<Expr, Integer> locals = new HashMap<>();
	private Environment environment = globals;

	Interpreter() {
		this.globals.define("clock", new Native("clock", 0) {
			@Override
			public Object call(Interpreter intp, List<Object> args) {
				return (double) System.currentTimeMillis() / 1000.0;
			}
		});

		this.globals.define("puts", new Native("puts", 1) {
			@Override
			public Object call(Interpreter intp, List<Object> args) {
				System.out.println(intp.stringify(args.get(0)));
				return null;
			}
		});

		this.globals.define("gets", new Native("gets", 0) {
			@SuppressWarnings("resource")
			@Override
			public Object call(Interpreter intp, List<Object> args) {
				return new java.util.Scanner(System.in).nextLine();
			}
		});

		this.globals.define("toString", new Native("toString", 1) {
			@Override
			public Object call(Interpreter intp, List<Object> args) {
				return intp.stringify(args.get(0));
			}
		});

		this.globals.define("toNumber", new Native("toNumber", 1) {
			@Override
			public Object call(Interpreter intp, List<Object> args) {
				var arg = args.get(0);
				if (arg instanceof String)
					return Scanner.toNumber((String) arg);
				else if (arg instanceof Double)
					return arg;
				return Double.NaN;
			}
		});

		this.globals.define("Data", new LoxClass("Data", new HashMap<String, LoxFunction>()));
	}

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

	void resolve(Expr expr, int depth) {
		this.locals.put(expr, depth);
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
	public Void visitFunctionStmt(Stmt.Function stmt) {
		var function = new LoxFunction(stmt, this.environment, false);
		this.environment.define(stmt.name, function);
		return null;
	}

	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null)
			value = this.evaluate(stmt.value);
		throw new Return(value);
	}

	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		this.environment.define(stmt.name, null);

		var methods = new HashMap<String, LoxFunction>();
		for (var method : stmt.methods) {
			var isInitializer = method.name.lexeme == "init";
			var function = new LoxFunction(method, this.environment, isInitializer);
			methods.put(method.name.lexeme, function);
		}

		var class_ = new LoxClass(stmt.name.lexeme, methods);
		this.environment.assign(stmt.name, class_);
		return null;
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

		Integer distance = this.locals.get(expr);
		if (distance != null) {
			this.environment.assignAt(distance, expr.name, value);
		} else {
			this.globals.assign(expr.name, value);
		}

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
		return this.lookupVariable(expr.name, expr);
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = this.evaluate(expr.right);

		switch (expr.operator.type) {
		case BANG:
			return !this.isTruthy(right);
		case MINUS:
			this.checkNumberOperand(expr.operator, right);
			return -(double) right;
		default:
			// unreachable
			return null;
		}
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = this.evaluate(expr.left);
		Object right = this.evaluate(expr.right);

		switch (expr.operator.type) {
		case EQUAL_EQUAL:
			return this.isEqual(left, right);
		case BANG_EQUAL:
			return !this.isEqual(left, right);
		case GREATER:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left > (double) right;
		case GREATER_EQUAL:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left >= (double) right;
		case LESS:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left < (double) right;
		case LESS_EQUAL:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left <= (double) right;
		case PLUS:
			if (left instanceof Double && right instanceof Double) {
				return (double) left + (double) right;
			}
			if (left instanceof String && right instanceof String) {
				return (String) left + (String) right;
			}
			throw new RuntimeError(expr.operator,
				"All operands must be either numbers or strings."
			);
		case MINUS:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left - (double) right;
		case SLASH:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left / (double) right;
		case STAR:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left * (double) right;
		case STAR_STAR:
			this.checkNumberOperands(expr.operator, left, right);
			return Math.pow((double) left, (double) right);
		case PERCENT:
			this.checkNumberOperands(expr.operator, left, right);
			return (double) left % (double) right;
		default:
			// unreachable
			return null;
		}
	}

	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = this.evaluate(expr.left);

		if (expr.operator.type == TokenType.OR)
			if (this.isTruthy(left))
				return left;
			else if (expr.operator.type == TokenType.AND)
				if (!this.isTruthy(left))
					return left;

		return this.evaluate(expr.right);
	}

	@Override
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = this.evaluate(expr.callee);
		if (!(callee instanceof LoxCallable))
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");

		List<Object> arguments = new ArrayList<>();
		for (Expr argument : expr.arguments) {
			arguments.add(this.evaluate(argument));
		}

		LoxCallable function = (LoxCallable) callee;
		if (arguments.size() != function.arity())
			throw new RuntimeError(expr.paren,
				"Expected " + function.arity() + "arguments but got " + arguments.size()
			);
		return function.call(this, arguments);
	}

	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object obj = this.evaluate(expr.object);
		if (obj instanceof LoxInstance)
			return ((LoxInstance) obj).get(expr.name);

		throw new RuntimeError(expr.name, "Only instances have properties.");
	}

	@Override
	public Object visitSetExpr(Expr.Set expr) {
		Object obj = this.evaluate(expr.object);
		if (!(obj instanceof LoxInstance))
			throw new RuntimeError(expr.name, "Only instances have fields.");

		Object value = this.evaluate(expr.value);
		((LoxInstance) obj).set(expr.name, value);

		return value;
	}

	@Override
	public Object visitThisExpr(Expr.This expr) {
		return this.lookupVariable(expr.keyword, expr);
	}

	private Object lookupVariable(Token name, Expr expr) {
		Integer distance = this.locals.get(expr);
		if (distance != null) {
			return this.environment.getAt(distance, name);
		} else {
			return this.environment.get(name);
		}
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double)
			return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}

	private boolean isTruthy(Object object) {
		if (object == null)
			return false;
		if (object instanceof Boolean)
			return (boolean) object;
		return true;
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double)
			return;
		throw new RuntimeError(operator, "Operands must be numbers.");
	}

	private boolean isEqual(Object left, Object right) {
		if (left == null && right == null)
			return true;
		if (left == null)
			return false;

		return left.equals(right);
	}

	private String stringify(Object object) {
		if (object == null)
			return "nil";

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