
import os, glob

matches = []
for root, dirs, files in os.walk('jar_extracted'):
    for file in files:
        if file.endswith('.class'):
            p = os.path.join(root, file)
            with open(p, 'rb') as cf:
                content = cf.read()
                if b'am' in content and b'z' in content:
                    # check for fieldref am.z
                    matches.append(p)

print('Found matches:', len(matches))
for m in matches[:20]:
    print(m)
