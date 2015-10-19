# coding=utf8
import array

"""
Convert Wikipedia SQL dumps into more usable format.
"""

page_dump_filename = 'enwiki-20151002-page.sql'
pagelinks_dump_filename = 'enwiki-20151002-pagelinks.sql'

converted_page_dump_filename = 'title_id_dict.txt'

def convert_page_dump():
	title_id_dict = {}

	print 'Reading page dump...'

	with open(page_dump_filename) as f:
		for line in f:
			if not line.startswith("INSERT INTO"):
				continue
			line = line[line.index("(") + 1:]
			pages = line.split("),(")
			for page_row in pages:
				page_metadata = page_row.split(",")
				if page_metadata[1] == "0": # 0 is the article namespace
					# Title -> ID
					title_id_dict[page_metadata[2]] = int(page_metadata[0])
			print len(title_id_dict.keys()), 'pages'

	print 'Writing to file...'

	converted_file = open('./' + converted_page_dump_filename, 'w+')
	for key in title_id_dict:
		converted_file.write(key + ' ' + str(title_id_dict[key]) + '\n')
	converted_file.close()

	return title_id_dict

def restore_converted_page_dump():
	title_id_dict = {}

	print 'Restoring converted page dump...'

	with open(converted_page_dump_filename) as f:
		lines = f.readlines()
		for line in lines:
			arr = line.split(' ')
			title_id_dict[arr[0]] = int(arr[1])

	print 'Done'
	
	return title_id_dict

title_id_dict = restore_converted_page_dump()

from_list = array.array('L')
to_list = array.array('L')

print 'Reading page links dump...'

with open(pagelinks_dump_filename) as f:
	for line in f:
		if not line.startswith("INSERT INTO"):
			continue
		line = line[line.index("(") + 1:]
		pagelinks = line.split("),(")
		for pagelink_row in pagelinks:
			pagelink_metadata = pagelink_row.split(",")
			if pagelink_metadata[1] == "0": # 0 is the article namespace
				from_id = int(pagelink_metadata[0])
				try:
					to_id = title_id_dict[pagelink_metadata[2]]
					from_list.append(from_id)
					to_list.append(to_id)
				except KeyError:
					pass
		print len(from_list), 'links'

print 'Writing to file...'

converted_file = open('./pagelinks_list.txt', 'w+')
for i in xrange(len(from_list)):
	converted_file.write(str(from_list[i]) + ' ' + str(to_list[i]) + '\n')
converted_file.close()

