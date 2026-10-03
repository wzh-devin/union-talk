"""Python 文件头与函数 Docstring 契约测试。

Author: devin
GitHub: https://github.com/wzh-devin
Version: 1.0.0
Since: 1.0.0
Created: 2026/07/30 11:17
"""

from __future__ import annotations

import ast
import re
from pathlib import Path

PROJECT_ROOT = Path(__file__).parents[1]
SOURCE_ROOT_LIST = [
    PROJECT_ROOT / "src",
    PROJECT_ROOT / "tests",
    PROJECT_ROOT / "alembic",
]
GENERATED_DIRECTORY_NAME = "generated"
REQUIRED_MODULE_METADATA_LIST = [
    "Author: devin",
    "GitHub: https://github.com/wzh-devin",
    "Version: 1.0.0",
    "Since: 1.0.0",
]
CREATED_PATTERN = re.compile(r"^Created: \d{4}/\d{2}/\d{2} \d{2}:\d{2}$", re.MULTILINE)
REST_PARAMETER_PATTERN = re.compile(
    r"^:param\s+([A-Za-z_]\w*):\s+\S.*$",
    re.MULTILINE,
)
REST_RETURN_PATTERN = re.compile(r"^:return:\s+\S.*$", re.MULTILINE)
REST_YIELD_PATTERN = re.compile(r"^:yield:\s+\S.*$", re.MULTILINE)
REST_RAISE_PATTERN = re.compile(
    r"^:raises\s+[A-Za-z_][\w.]*:\s+\S.*$",
    re.MULTILINE,
)
LEGACY_SECTION_PATTERN = re.compile(
    r"^(Args|Returns|Yields|Raises):$",
    re.MULTILINE,
)
REDUNDANT_TYPE_FIELD_PATTERN = re.compile(
    r"^:(type\s+\w+|rtype):",
    re.MULTILINE,
)


def _maintained_python_file_list() -> list[Path]:
    """
    查询所有人工维护的 Python 文件

    :return: 按路径排序的 Python 文件列表
    """

    return sorted(
        file_path
        for source_root in SOURCE_ROOT_LIST
        for file_path in source_root.rglob("*.py")
        if GENERATED_DIRECTORY_NAME not in file_path.parts
    )


def _function_node_list(module_node: ast.Module) -> list[ast.FunctionDef | ast.AsyncFunctionDef]:
    """
    查询模块内全部同步与异步函数节点

    :param module_node: 已解析的 Python 模块节点
    :return: 按源码位置排序的函数节点列表
    """

    return sorted(
        (
            node
            for node in ast.walk(module_node)
            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef))
        ),
        key=lambda node: (node.lineno, node.col_offset),
    )


def _documented_parameter_name_list(docstring: str) -> list[str]:
    """
    按出现顺序解析 reStructuredText 参数字段

    :param docstring: 待解析的函数 Docstring
    :return: 已写参数说明的名称列表
    """

    return REST_PARAMETER_PATTERN.findall(docstring)


def _parameter_name_list(
    function_node: ast.FunctionDef | ast.AsyncFunctionDef,
) -> list[str]:
    """
    按签名顺序查询需要文档说明的参数名称

    :param function_node: 待检查的函数节点
    :return: 除 self 和 cls 外的参数名称列表
    """

    parameter_name_list = [
        argument.arg
        for argument in (
            *function_node.args.posonlyargs,
            *function_node.args.args,
            *function_node.args.kwonlyargs,
        )
        if argument.arg not in {"self", "cls"}
    ]
    if function_node.args.vararg is not None:
        parameter_name_list.append(function_node.args.vararg.arg)
    if function_node.args.kwarg is not None:
        parameter_name_list.append(function_node.args.kwarg.arg)
    return parameter_name_list


def _is_generator_function(
    function_node: ast.FunctionDef | ast.AsyncFunctionDef,
) -> bool:
    """
    判断函数返回注解是否表示生成器契约

    :param function_node: 待检查的函数节点
    :return: 是否为生成器函数
    """

    return_annotation = function_node.returns
    while isinstance(return_annotation, ast.Subscript):
        return_annotation = return_annotation.value
    annotation_name = (
        return_annotation.id
        if isinstance(return_annotation, ast.Name)
        else return_annotation.attr
        if isinstance(return_annotation, ast.Attribute)
        else ""
    )
    return annotation_name in {
        "AsyncGenerator",
        "AsyncIterator",
        "Generator",
        "Iterator",
    }


def _has_standard_source_layout(
    source_line_list: list[str],
    function_node: ast.FunctionDef | ast.AsyncFunctionDef,
) -> bool:
    """
    校验函数 Docstring 的三引号独占首尾行

    :param source_line_list: Python 源文件行列表
    :param function_node: 待检查的函数节点
    :return: 是否符合统一排版
    """

    docstring_statement = function_node.body[0]
    if not isinstance(docstring_statement, ast.Expr) or docstring_statement.end_lineno is None:
        return False
    opening_line = source_line_list[docstring_statement.lineno - 1].strip()
    closing_line = source_line_list[docstring_statement.end_lineno - 1].strip()
    return opening_line == '"""' and closing_line == '"""'


def test_all_python_files_have_standard_module_header() -> None:
    """
    校验所有人工维护文件具有完整标准文件头

    :return: 无返回值
    """

    violation_list: list[str] = []
    for file_path in _maintained_python_file_list():
        module_node = ast.parse(file_path.read_text(encoding="utf-8"))
        docstring = ast.get_docstring(module_node, clean=True)
        relative_path = file_path.relative_to(PROJECT_ROOT)
        if not docstring:
            violation_list.append(f"{relative_path}: 缺少模块 Docstring")
            continue
        if not docstring.splitlines()[0].strip():
            violation_list.append(f"{relative_path}: 缺少文件职责描述")
        for metadata in REQUIRED_MODULE_METADATA_LIST:
            if metadata not in docstring:
                violation_list.append(f"{relative_path}: 缺少 {metadata}")
        if CREATED_PATTERN.search(docstring) is None:
            violation_list.append(f"{relative_path}: Created 格式不正确")

    assert not violation_list, "\n" + "\n".join(violation_list)


def test_all_python_functions_have_complete_docstring() -> None:
    """
    校验所有函数符合 reStructuredText Docstring 契约

    :return: 无返回值
    """

    violation_list: list[str] = []
    for file_path in _maintained_python_file_list():
        source = file_path.read_text(encoding="utf-8")
        source_line_list = source.splitlines()
        module_node = ast.parse(source)
        relative_path = file_path.relative_to(PROJECT_ROOT)
        for function_node in _function_node_list(module_node):
            location = f"{relative_path}:{function_node.lineno}:{function_node.name}"
            docstring = ast.get_docstring(function_node, clean=True)
            if not docstring:
                violation_list.append(f"{location}: 缺少函数 Docstring")
                continue
            if not docstring.splitlines()[0].strip():
                violation_list.append(f"{location}: 缺少职责说明")
            if not _has_standard_source_layout(source_line_list, function_node):
                violation_list.append(f"{location}: 三引号必须独占 Docstring 首尾行")
            legacy_section_list = LEGACY_SECTION_PATTERN.findall(docstring)
            if legacy_section_list:
                legacy_sections = ", ".join(legacy_section_list)
                violation_list.append(f"{location}: 禁止使用 {legacy_sections}")
            expected_parameter_name_list = _parameter_name_list(function_node)
            documented_parameter_name_list = _documented_parameter_name_list(docstring)
            if documented_parameter_name_list != expected_parameter_name_list:
                expected_names = ", ".join(expected_parameter_name_list) or "无"
                actual_names = ", ".join(documented_parameter_name_list) or "无"
                violation_list.append(
                    f"{location}: :param 顺序或内容不正确，"
                    f"期望 [{expected_names}]，实际 [{actual_names}]"
                )
            has_return = REST_RETURN_PATTERN.search(docstring) is not None
            has_yield = REST_YIELD_PATTERN.search(docstring) is not None
            if _is_generator_function(function_node):
                if not has_yield or has_return:
                    violation_list.append(f"{location}: 生成器必须且只能使用 :yield:")
            elif not has_return or has_yield:
                violation_list.append(f"{location}: 普通函数必须且只能使用 :return:")
            if ":raises " in docstring and REST_RAISE_PATTERN.search(docstring) is None:
                violation_list.append(f"{location}: :raises 字段格式或说明不完整")
            if REDUNDANT_TYPE_FIELD_PATTERN.search(docstring) is not None:
                violation_list.append(f"{location}: 禁止重复维护 :type 或 :rtype:")

    assert not violation_list, "\n" + "\n".join(violation_list)
