#!/usr/bin/python3

from pathlib import Path
from typing import Dict


def define_type(base_name, name, fields: Dict):
    field_decls = '\n\t\t'.join(f'final {typ} {name};'
                                for name, typ in fields.items())
    constr_args = ', '.join(f'{typ} {name}'
                            for name, typ in fields.items())
    constr_body = '\n\t\t\t'.join(f'this.{name} = {name};'
                                  for name in fields.keys())
    return '''
\tstatic class {n} extends {bn} {{
\t\t{f}

\t\t{n}({ca}) {{
\t\t\t{cb}
\t\t}}
    
\t\t@Override
\t\t<R> R accept(Visitor<R> visitor) {{
\t\t\treturn visitor.visit{n}{bn}(this);
\t\t}}
\t}}
'''.format(n=name, bn=base_name, f=field_decls, ca=constr_args, cb=constr_body)

def define_visitor(base_name, types: Dict):
    ast_visitors = '\n\t\t'.join(f'R visit{type_name}{base_name}({type_name} {base_name.lower()});'
                                 for type_name in types.keys())
    return '''\
\tinterface Visitor<R> {{
\t\t{v}
\t}}
'''.format(v=ast_visitors)

def define_ast(output_dir, base_name, types: Dict):
    with open(output_dir/f'{base_name}.java', encoding='utf-8', mode='w') as f:
        ast_classes = ''.join(define_type(base_name, type_name, fields)
                              for type_name, fields in types.items())
        visitor = define_visitor(base_name, types)
        f.write('''\
package com.craftinginterpreters.lox;

import java.util.List;

abstract class {bn} {{
{v}
\tabstract<R> R accept(Visitor<R> visitor);
{acs}
}}
'''.format(bn=base_name, acs=ast_classes, v=visitor))


def main():
    output_dir = Path('..')/'src'/'com'/'craftinginterpreters'/'lox'
    define_ast(output_dir, 'Expr', {
        'Binary': {'left': 'Expr', 'operator': 'Token', 'right': 'Expr'},
        'Logical': {'left': 'Expr', 'operator': 'Token', 'right': 'Expr'},
        'Grouping': {'expression': 'Expr'},
        'Literal': {'value': 'Object'},
        'Unary': {'operator': 'Token', 'right': 'Expr'},
        'Variable': {'name': 'Token'},
        'Assign': {'name': 'Token', 'value': 'Expr'},
        'Call': {'callee': 'Expr', 'paren': 'Token', 'arguments': 'List<Expr>'},
        'Get': {'object': 'Expr', 'name': 'Token'},
        'Set': {'object': 'Expr', 'name': 'Token', 'value': 'Expr'},
    })
    define_ast(output_dir, 'Stmt', {
        'Block': {'statements': 'List<Stmt>'},
        'Expression': {'expression': 'Expr'},
        'Print': {'expression': 'Expr'},
        'Var': {'name': 'Token', 'initializer': 'Expr'},
        'If': {'condition': 'Expr', 'thenBranch': 'Stmt', 'elseBranch': 'Stmt'},
        'While': {'condition': 'Expr', 'body': 'Stmt'},
        'Function': {'name': 'Token', 'params': 'List<Token>', 'body': 'List<Stmt>'},
        'Return': {'keyword': 'Token' , 'value': 'Expr'},
        'Class': {'name': 'Token', 'methods': 'List<Stmt.Function>'},
    })

if __name__ == '__main__':
    main()
