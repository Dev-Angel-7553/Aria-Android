import os, re, json, sys
root = sys.argv[1]
cyr = re.compile('[А-Яа-яЁё]')
keys = {}
def parse_literal(src, i):
    # src[i] == '"', return (end_index_after, pieces) where pieces: list of ('t',text)|('e',expr)
    assert src[i] == '"'
    if src.startswith('"""', i):
        return None, None
    i += 1
    out = []; buf = ''
    while i < len(src):
        c = src[i]
        if c == '\\':
            nxt = src[i+1]
            buf += {'n':'\n','t':'\t','"':'"','\\':'\\','$':'$',"'":"'"}.get(nxt, '\\'+nxt)
            if nxt == 'u':
                buf = buf[:-2] + chr(int(src[i+2:i+6],16)); i += 6; continue
            i += 2; continue
        if c == '"':
            if buf: out.append(('t', buf))
            return i+1, out
        if c == '$' and i+1 < len(src) and src[i+1] == '{':
            if buf: out.append(('t', buf)); buf=''
            depth = 1; j = i+2
            while depth:
                if src[j] == '{': depth += 1
                elif src[j] == '}': depth -= 1
                elif src[j] == '"':
                    k, _ = parse_literal(src, j)
                    if k is None: return len(src), []
                    j = k; continue
                j += 1
            out.append(('e', src[i+2:j-1])); i = j; continue
        if c == '$' and i+1 < len(src) and (src[i+1].isalpha() or src[i+1]=='_'):
            if buf: out.append(('t', buf)); buf=''
            m = re.match(r'[A-Za-z_][A-Za-z0-9_]*', src[i+1:])
            out.append(('e', m.group(0))); i += 1+len(m.end() and m.group(0)); continue
        if c == '\n':
            return i, []
        buf += c; i += 1
    return i, []
for dp, _, fs in os.walk(root):
    if '/lyrics' in dp: continue
    for f in fs:
        if not f.endswith('.kt'): continue
        p = os.path.join(dp, f); src = open(p, encoding='utf-8').read()
        i = 0
        while True:
            i = src.find('"', i)
            if i < 0: break
            # skip char literals like '"'
            if i > 0 and src[i-1] == "'" and src[i+1:i+2] == "'":
                i += 2; continue
            if src.startswith('"""', i):
                j = src.find('"""', i+3); i = j+3 if j >= 0 else len(src); continue
            # skip comments-free code; ignore strings in line comments roughly
            end, pieces = parse_literal(src, i)
            if end is None: i += 3; continue
            text = ''; n = 0
            for kind, v in pieces:
                if kind == 't': text += v
                else: text += '{%d}' % n; n += 1
            if cyr.search(text):
                keys.setdefault(text, os.path.relpath(p, root))
            i = max(end, i+1)
print(len(keys), file=sys.stderr)
json.dump(keys, open(sys.argv[2], 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
