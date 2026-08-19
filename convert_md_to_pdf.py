# -*- coding: utf-8 -*-
"""
将Markdown格式的需求规格说明书转换为排版精美的PDF文档
使用 fpdf2 库（纯Python实现，无需外部依赖）
"""

from fpdf import FPDF
from fpdf.enums import XPos, YPos, Align
import re
import os
import textwrap

class MarkdownToPDF(FPDF):
    """自定义Markdown转PDF类"""
    
    def __init__(self):
        super().__init__()
        # 设置中文字体（使用系统自带的微软雅黑）
        self.add_font('MicrosoftYaHei', '', 'C:/Windows/Fonts/msyh.ttc', uni=True)
        self.add_font('MicrosoftYaHei', 'B', 'C:/Windows/Fonts/msyhbd.ttc', uni=True)
        self.add_font('Consolas', '', 'C:/Windows/Fonts/consola.ttf', uni=True)
        
        # 配置参数
        self.set_auto_page_break(auto=True, margin=25)
        
        # 颜色定义
        self.brand_green = (14, 143, 120)      # #0e8f78
        self.dark_blue = (18, 48, 68)          # #123044
        self.accent_green = (29, 199, 163)     # #1dc7a3
        self.text_dark = (16, 33, 43)         # #10212b
        self.text_gray = (108, 125, 137)      # #6c7d89
        self.light_bg = (238, 243, 247)       # #eef3f7
        
    def header(self):
        """页眉"""
        if self.page_no() > 1:
            self.set_font('MicrosoftYaHei', '', 8)
            self.set_text_color(*self.text_gray)
            self.cell(0, 10, '鑫速录 - 企业财务报表自动化填报与智能分析系统 | 产品需求规格说明书 v2.0', 
                     align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.line(10, 15, 200, 15)
            self.ln(5)
    
    def footer(self):
        """页脚"""
        self.set_y(-20)
        self.set_font('MicrosoftYaHei', '', 9)
        self.set_text_color(*self.text_gray)
        if self.page_no() > 1:
            self.cell(0, 10, f'第 {self.page_no()} 页 / 共 {{nb}} 页', align='C')
    
    def add_cover_page(self):
        """添加封面页"""
        self.add_page()
        
        # 空行
        for _ in range(8):
            self.ln()
        
        # 主标题
        self.set_font('MicrosoftYaHei', 'B', 42)
        self.set_text_color(*self.brand_green)
        self.cell(0, 25, '鑫速录', align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        
        self.ln(10)
        
        # 副标题
        self.set_font('MicrosoftYaHei', '', 18)
        self.set_text_color(*self.text_dark)
        subtitle = '企业财务报表自动化填报与智能分析系统'
        self.cell(0, 12, subtitle, align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        
        self.ln(5)
        
        # 文档类型
        self.set_font('MicrosoftYaHei', 'B', 16)
        self.set_text_color(*self.accent_green)
        self.cell(0, 12, '产品需求规格说明书 (PRD)', align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        
        self.ln(30)
        
        # 分隔线
        self.set_draw_color(*self.brand_green)
        self.set_line_width(1)
        self.line(60, self.get_y(), 150, self.get_y())
        
        self.ln(40)
        
        # 文档信息
        self.set_font('MicrosoftYaHei', '', 12)
        self.set_text_color(*self.text_gray)
        info_items = [
            '文档版本：v2.0',
            '编写日期：2026年8月5日',
            '文档状态：正式发布',
            '适用范围：微信小程序端 + Vue管理后台 + Spring Boot后端'
        ]
        for item in info_items:
            self.cell(0, 10, item, align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
    
    def add_toc_page(self, toc_items):
        """添加目录页"""
        self.add_page()
        
        # 目录标题
        self.set_font('MicrosoftYaHei', 'B', 24)
        self.set_text_color(*self.brand_green)
        self.cell(0, 20, '目  录', align='C', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        
        self.ln(10)
        
        # 目录项
        self.set_font('MicrosoftYaHei', '', 11)
        self.set_text_color(*self.text_dark)
        
        for item, page in toc_items:
            # 计算缩进（根据层级）
            indent = 0
            if item.startswith('  ') or item.startswith('\t'):
                indent = 10
            
            # 绘制目录项
            self.set_x(self.l_margin + indent)
            
            # 清理文本（移除markdown标记）
            clean_text = re.sub(r'[#*`]', '', item).strip()
            
            # 如果是主标题，加粗显示
            if not item.startswith(' ') and not item.startswith('\t'):
                self.set_font('MicrosoftYaHei', 'B', 11)
            else:
                self.set_font('MicrosoftYaHei', '', 10)
            
            # 显示文本和页码（右对齐）
            self.multi_cell(160, 8, clean_text, border=0, new_x=XPos.RIGHT, new_y=YPos.TOP, max_line_height=8)
            self.set_font('MicrosoftYaHei', '', 10)
            self.cell(20, 8, str(page), align='R', new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            
            self.ln(2)
    
    def process_markdown(self, md_content):
        """处理Markdown内容并添加到PDF"""
        lines = md_content.split('\n')
        i = 0
        
        while i < len(lines):
            line = lines[i]
            
            # 跳过空行
            if not line.strip():
                i += 1
                continue
            
            # 处理标题
            if line.startswith('#'):
                self.process_heading(line)
                i += 1
                continue
            
            # 处理表格
            if '|' in line and i + 1 < len(lines) and '|---' in lines[i + 1]:
                i = self.process_table(lines, i)
                continue
            
            # 处理代码块
            if line.strip().startswith('```'):
                i = self.process_code_block(lines, i)
                continue
            
            # 处理列表
            if line.strip().startswith('- ') or line.strip().startswith('* '):
                i = self.process_list(lines, i)
                continue
            
            # 处理有序列表
            if re.match(r'^\d+\.\s', line.strip()):
                i = self.process_ordered_list(lines, i)
                continue
            
            # 处理引用块
            if line.startswith('>'):
                i = self.process_blockquote(lines, i)
                continue
            
            # 处理分隔线
            if line.strip() in ('---', '***', '___'):
                self.draw_hr()
                i += 1
                continue
            
            # 处理普通段落
            if line.strip():
                self.process_paragraph(line)
            
            i += 1
    
    def process_heading(self, line):
        """处理标题"""
        level = len(re.match(r'^#+', line).group())
        text = re.sub(r'^#+\s*', '', line).strip()
        text = re.sub(r'[*_`#]', '', text)  # 移除markdown标记
        
        # 根据级别设置样式
        if level == 1:
            self.set_font('MicrosoftYaHei', 'B', 22)
            self.set_text_color(*self.brand_green)
            self.ln(8)
            self.multi_cell(0, 14, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            # 添加下划线
            self.set_draw_color(*self.brand_green)
            self.set_line_width(0.8)
            y = self.get_y()
            self.line(self.l_margin, y, self.w - self.r_margin, y)
            self.ln(8)
            
        elif level == 2:
            self.set_font('MicrosoftYaHei', 'B', 17)
            self.set_text_color(*self.dark_blue)
            self.ln(6)
            # 左侧色条
            x = self.get_x()
            y = self.get_y()
            self.set_fill_color(*self.brand_green)
            self.rect(x, y, 4, 10, 'F')
            self.set_x(x + 8)
            self.multi_cell(0, 11, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.ln(5)
            
        elif level == 3:
            self.set_font('MicrosoftYaHei', 'B', 14)
            self.set_text_color(*self.text_dark)
            self.ln(4)
            self.multi_cell(0, 10, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.ln(3)
            
        elif level >= 4:
            self.set_font('MicrosoftYaHei', 'B', 12)
            self.set_text_color(*self.text_gray)
            self.ln(3)
            self.multi_cell(0, 9, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            self.ln(2)
    
    def process_paragraph(self, line):
        """处理普通段落"""
        # 移除markdown标记
        text = re.sub(r'[*_`]', '', line).strip()
        
        # 处理粗体和斜体
        text = re.sub(r'\*\*(.+?)\*\*', r'\1', text)
        text = re.sub(r'\*(.+?)\*', r'\1', text)
        
        self.set_font('MicrosoftYaHei', '', 10)
        self.set_text_color(*self.text_dark)
        
        # 自动换行处理长文本
        self.multi_cell(0, 7, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        self.ln(3)
    
    def process_table(self, lines, start_idx):
        """处理表格"""
        table_lines = []
        i = start_idx
        
        # 收集表格所有行
        while i < len(lines):
            line = lines[i].strip()
            if not line:
                break
            if line.startswith('|'):
                cells = [cell.strip() for cell in line.split('|')[1:-1]]
                # 跳过分隔行
                if not all(set(cell) <= set('-: ') for cell in cells):
                    table_lines.append(cells)
            else:
                break
            i += 1
        
        if not table_lines:
            return i + 1
        
        # 计算列宽
        num_cols = len(table_lines[0])
        available_width = self.w - self.l_margin - self.r_margin
        col_width = available_width / num_cols
        
        # 绘制表格
        for row_idx, row in enumerate(table_lines):
            # 表头样式
            if row_idx == 0:
                self.set_fill_color(*self.brand_green)
                self.set_text_color(255, 255, 255)
                self.set_font('MicrosoftYaHei', 'B', 9)
            else:
                # 斑马纹
                if row_idx % 2 == 0:
                    self.set_fill_color(245, 245, 245)
                else:
                    self.set_fill_color(255, 255, 255)
                self.set_text_color(*self.text_dark)
                self.set_font('MicrosoftYaHei', '', 9)
            
            max_height = 8
            for col_idx, cell in enumerate(row):
                # 清理单元格内容
                cell_text = re.sub(r'[*_`]', '', cell).strip()
                
                x = self.l_margin + col_idx * col_width
                
                # 绘制单元格背景
                self.rect(x, self.get_y(), col_width, max_height, 'F')
                
                # 写入文字
                self.set_xy(x + 2, self.get_y() + 1.5)
                # 截断过长的文本
                display_text = cell_text[:int(col_width / 2.5)] if len(cell_text) > int(col_width / 2.5) else cell_text
                self.cell(col_width - 4, 6, display_text, border=0)
            
            self.ln(max_height)
        
        self.ln(5)
        return i
    
    def process_code_block(self, lines, start_idx):
        """处理代码块"""
        code_lines = []
        i = start_idx + 1  # 跳过开始的```
        
        while i < len(lines):
            if lines[i].strip().startswith('```'):
                break
            code_lines.append(lines[i])
            i += 1
        
        # 代码块背景
        self.set_fill_color(40, 44, 52)
        y_start = self.get_y()
        
        # 绘制背景矩形（预估高度）
        num_lines = len(code_lines)
        block_height = min(num_lines * 5 + 10, 150)  # 最大高度限制
        
        self.rect(self.l_margin, y_start, self.w - self.l_margin - self.r_margin, 
                  block_height, 'F')
        
        # 写入代码
        self.set_xy(self.l_margin + 5, y_start + 5)
        self.set_font('Consolas', '', 8)
        self.set_text_color(171, 178, 191)  # 浅灰色代码文字
        
        for line in code_lines[:30]:  # 限制最多显示30行
            # 截断过长行
            display_line = line[:90] if len(line) > 90 else line
            self.cell(0, 5, display_line, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            if self.get_y() > y_start + block_height - 10:
                break
        
        self.set_y(y_start + block_height + 5)
        self.ln(3)
        
        return i + 1
    
    def process_list(self, lines, start_idx):
        """处理无序列表"""
        i = start_idx
        
        while i < len(lines):
            line = lines[i].strip()
            
            if not line.startswith('- ') and not line.startswith('* ') and not line.startswith('  -') and not line.startswith('  *'):
                break
            
            # 计算缩进级别
            indent_level = (len(line) - len(line.lstrip())) // 2
            text = re.sub(r'^[-*]\s*', '', line).strip()
            text = re.sub(r'[*_`]', '', text)
            
            self.set_font('MicrosoftYaHei', '', 10)
            self.set_text_color(*self.text_dark)
            
            x = self.l_margin + 10 + (indent_level * 5)
            self.set_x(x)
            
            # 项目符号
            self.set_fill_color(*self.brand_green)
            y = self.get_y() + 3
            self.ellipse(x - 5, y, 2, 2, 'F')
            
            # 文本
            self.set_x(x + 3)
            self.multi_cell(self.w - x - self.r_margin - 3, 7, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            
            i += 1
        
        self.ln(2)
        return i
    
    def process_ordered_list(self, lines, start_idx):
        """处理有序列表"""
        i = start_idx
        counter = 1
        
        while i < len(lines):
            line = lines[i].strip()
            
            if not re.match(r'^\d+\.\s', line) and not line.startswith('  '):
                if not re.match(r'^\s+\d+\.\s', line):
                    break
            
            # 提取序号和文本
            match = re.match(r'^(\d+)\.\s*(.*)', line.lstrip())
            if match:
                counter = int(match.group(1))
                text = match.group(2)
            else:
                text = re.sub(r'^\s*\d+\.\s*', '', line).strip()
            
            text = re.sub(r'[*_`]', '', text)
            
            indent_level = (len(line) - len(line.lstrip())) // 2
            
            self.set_font('MicrosoftYaHei', '', 10)
            self.set_text_color(*self.text_dark)
            
            x = self.l_margin + 10 + (indent_level * 5)
            self.set_x(x)
            
            # 序号
            self.set_font('MicrosoftYaHei', 'B', 10)
            self.set_text_color(*self.brand_green)
            self.cell(8, 7, f'{counter}.', new_x=XPos.RIGHT, new_y=YPos.TOP)
            
            # 文本
            self.set_font('MicrosoftYaHei', '', 10)
            self.set_text_color(*self.text_dark)
            self.multi_cell(self.w - x - self.r_margin - 8, 7, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)
            
            counter += 1
            i += 1
        
        self.ln(2)
        return i
    
    def process_blockquote(self, lines, start_idx):
        """处理引用块"""
        quote_lines = []
        i = start_idx
        
        while i < len(lines):
            if lines[i].startswith('>'):
                quote_lines.append(lines[i][1:].strip())
                i += 1
            else:
                break
        
        if not quote_lines:
            return i + 1
        
        # 引用块背景
        y_start = self.get_y()
        self.set_fill_color(240, 248, 246)
        
        # 左侧边框
        self.set_fill_color(*self.brand_green)
        self.rect(self.l_margin, y_start, 4, len(quote_lines) * 7 + 10, 'F')
        
        # 背景
        self.set_fill_color(240, 248, 246)
        self.rect(self.l_margin + 4, y_start, self.w - self.l_margin - self.r_margin - 4, 
                  len(quote_lines) * 7 + 10, 'F')
        
        # 引用文字
        self.set_xy(self.l_margin + 12, y_start + 5)
        self.set_font('MicrosoftYaHei', '', 10)
        self.set_text_color(85, 85, 85)
        
        for line in quote_lines:
            text = re.sub(r'[*_>`]', '', line)
            self.multi_cell(self.w - self.l_margin - self.r_margin - 16, 7, text, 
                           new_x=XPos.LMARGIN, new_y=YPos.NEXT)
        
        self.set_y(y_start + len(quote_lines) * 7 + 15)
        return i
    
    def draw_hr(self):
        """绘制分隔线"""
        y = self.get_y()
        self.set_draw_color(*self.brand_green)
        self.set_line_width(0.5)
        self.line(self.l_margin + 20, y, self.w - self.r_margin - 20, y)
        self.ln(8)


def extract_toc(md_content):
    """从Markdown中提取目录项"""
    toc_items = []
    current_section = 0
    
    for line in md_content.split('\n'):
        if line.startswith('#'):
            # 统计#号数量确定层级
            level = len(re.match(r'^#+', line).group())
            text = re.sub(r'^#+\s*', '', line).strip()
            
            if level <= 2:  # 只提取主要章节
                current_section += 1
                toc_items.append((text, current_section))
    
    return toc_items


def generate_pdf(md_filepath, pdf_filepath):
    """生成PDF文档的主函数"""
    print("=" * 70)
    print("开始转换 Markdown → PDF")
    print("=" * 70)
    
    # 1. 读取Markdown文件
    print(f"\n[1/5] 读取文件...")
    with open(md_filepath, 'r', encoding='utf-8') as f:
        md_content = f.read()
    print(f"      ✓ 成功读取 {len(md_content):,} 字符")
    
    # 2. 创建PDF对象
    print("\n[2/5] 初始化PDF引擎...")
    pdf = MarkdownToPDF()
    pdf.alias_nb_pages()
    print(f"      ✓ 字体加载完成（微软雅黑 + Consolas）")
    
    # 3. 添加封面
    print("\n[3/5] 生成封面...")
    pdf.add_cover_page()
    print(f"      ✓ 封面完成")
    
    # 4. 提取并添加目录
    print("\n[4/5] 生成目录...")
    toc_items = extract_toc(md_content)
    pdf.add_toc_page(toc_items)
    print(f"      ✓ 目录完成（{len(toc_items)} 个章节）")
    
    # 5. 处理正文内容
    print("\n[5/5] 转换正文内容...")
    pdf.process_markdown(md_content)
    print(f"      ✓ 正文转换完成")
    
    # 保存PDF
    print("\n" + "-" * 70)
    print(f"正在保存 PDF: {pdf_filepath}")
    pdf.output(pdf_filepath)
    
    # 验证
    if os.path.exists(pdf_filepath):
        file_size = os.path.getsize(pdf_filepath)
        size_mb = file_size / (1024 * 1024)
        size_kb = file_size / 1024
        
        print(f"\n{'=' * 70}")
        print(f"✓ PDF 生成成功！")
        print(f"{'=' * 70}")
        print(f"\n📄 输出文件：{pdf_filepath}")
        print(f"📊 文件大小：{size_mb:.2f} MB ({size_kb:.1f} KB)")
        print(f"📑 总页数：约 {pdf.page_no()} 页")
        print(f"\n提示：可以直接打开查看效果！")
        return True
    else:
        print("\n❌ PDF生成失败")
        return False


if __name__ == '__main__':
    # 文件路径配置
    base_dir = r'c:\Users\14761\Desktop\企业财务报表自动化填报与智能分析\鑫速录'
    md_file = '鑫速录产品需求规格说明书.md'
    pdf_file = '鑫速录产品需求规格说明书.pdf'
    
    md_path = os.path.join(base_dir, md_file)
    pdf_path = os.path.join(base_dir, pdf_file)
    
    # 执行转换
    success = generate_pdf(md_path, pdf_path)
    
    if not success:
        print("\n请检查错误信息并重试")
