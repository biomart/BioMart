#!/usr/bin/python
# [BM1102151350]
# usage:
#	on linux:
#		$ ./daemon_runner.py # using defaults
#	on windows:
#		Administrator@arektest /cygdrive/g/selenium
#		$ ./daemon_runner.bat # after cd-ing to /cygdrive/g/selenium as indicated by prompt
# note: MUST be started from cygwin (so that cygwin's ps -W picks up on it)
# 

#G:
#cd G:\\selenium
#daemon_runner.py

# ===========================================================================

import os,sys,time,subprocess

# ===========================================================================

DEFAULT_INPUT_FILE  =  sys.argv[0] + '.in'
DEFAULT_OUTPUT_FILE =  sys.argv[0] + '.out'
DEFAULT_END_OF_FILE = '_EOF_'

# ===========================================================================

def read_file(file):
	r=open(file, 'r')
	content = r.read()
	r.close()
	return content

# ===========================================================================

def write_file(file, content):
	w=open(file, 'w')
	w.write(content)
	w.close()

# ===========================================================================

def process_command(command, output_file, end_of_file):
	popen = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE) # [cBM1102151521]
	t = popen.communicate()
	exit_status=popen.returncode
	stdout = t[0]
	stderr = t[1]
	print "stdout = |" + str(stdout) + "|"
	print "stderr = |" + str(stderr) + "|"
	print "exit_status = " + str(exit_status)
	return exit_status
	
# ===========================================================================

def cycle(input_file, output_file, end_of_file):
	if os.path.isfile(input_file):
		content = read_file(input_file).strip()
		if content.endswith(end_of_file):
			command = content.replace(end_of_file,'')
			print "command = |" + command + "|"

			# remove input file (signals being processed)
			os.remove(input_file)
			assert not os.path.isfile(input_file)

			# run command
			exit_status = process_command(command, output_file, end_of_file)

			# write output file (signals processed)
			write_file(output_file, str(exit_status) + end_of_file) # cannot assert file has been written as it may be immediately picked up by calling program (potential condition race)
	
# ===========================================================================

def main():
	input_file = DEFAULT_INPUT_FILE if len(sys.argv)<4 else sys.argv[1]
	output_file = DEFAULT_OUTPUT_FILE if len(sys.argv)<4 else sys.argv[2]
	end_of_file = DEFAULT_END_OF_FILE if len(sys.argv)<4 else sys.argv[3]
	while True:
		cycle(input_file, output_file, end_of_file)
		time.sleep(1)

# ===========================================================================

if __name__ == '__main__':
	main()

# ===========================================================================
