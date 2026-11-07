import re
with open('src/pages/Admin/Dashboard.tsx', 'r', encoding='utf-8') as f:
    text = f.read()

bad_good = [
    ('Thá»‘ng kÃª', 'Th?ng k�'),
    ('tá»•ng quan', 't?ng quan'),
    ('Quáº£n LÂ½', 'Qu?n L?'),
    ('Quáº£n lÂ½', 'Qu?n l?'),
    ('ngÆ°á»� i dÃ¹ng', 'ng�?i d�ng'),
    ('PhÃ¢n Quyá»� n', 'Ph�n Quy?n'),
    ('Chi Tiáº¿t', 'Chi Ti?t'),
    ('Há»� c Liá»‡u', 'H?c Li?u'),
    ('Há»� c liá»‡u', 'H?c li?u'),
    ('Há»ŒC', 'H?C'),
    ('CHá»®', 'CH?'),
    ('CĂ� I', 'C�I'),
    ('Chá»¯ CÃ¡i', 'Ch? C�i'),
    ('Chá»‰nh Sá»�a', 'Ch?nh S?a'),
    ('Sá»�a chá»¯a', 'S?a ch?a'),
    ('ThĂªm', 'Th�m'),
    ('Má»›i', 'M?i'),
    ('TĂªn', 'T�n'),
    ('Chá»¯', 'Ch?'),
    ('CĂ¡i', 'C�i'),
    ('Tr?ng Thi', 'Tr?ng Th�i'),
    ('Tr?ng Thi', 'Tr?ng Th�i'),
    ('(?n/Hi?n)', '(?n/Hi?n)'),
    ('ang ?n', '�ang ?n'),
    ('H?c sinh', 'H?c sinh'),
    ('khng th?y', 'kh�ng th?y'),
    ('khng th?y', 'kh�ng th?y'),
    ('ang Hi?n', '�ang Hi?n'),
    ('h?c b?nh th?ng', 'h?c b?nh th�?ng'),
    ('h?c b?nh th?ng', 'h?c b?nh th�?ng'),
    ('thÃ´ng tin', 'th�ng tin'),
    ('Ã¢m thanh', '�m thanh'),
    ('máº«u', 'm?u'),
    ('cá»§a', 'c?a'),
    ('lÃªn server', 'l�n server'),
    ('Báº¥m vÃ� o', 'B?m v�o'),
    ('báº¥t ká»³', 'b?t k?'),
    ('đá»ƒ', '�?'),
    ('chi tiáº¿t', 'chi ti?t'),
    ('thÃªm', 'th�m'),
    ('Tráº¡ng ThÃ¡i', 'Tr?ng Th�i')
]

for bad, good in bad_good:
    text = text.replace(bad, good)

with open('src/pages/Admin/Dashboard.tsx', 'w', encoding='utf-8') as f:
    f.write(text)
print('Fixed text replacements')
