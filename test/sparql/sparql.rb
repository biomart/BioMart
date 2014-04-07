#!/usr/bin/ruby

require 'rubygems'
require 'optparse'
require 'sparql/client'

@host = 'central.biomart.org'
@port = 80

verify = false

def print_help
	puts 'Usage: sparql.rb [options]'
	puts ''
	puts 'Executes queries from .sparql files that are found in the current working directory.'
	puts 'If -v is given, then it also reads .reference files and compares the server results'
	puts 'to the contents of the .reference files.'
	puts ''
	puts 'Options:'
	puts "  -h HOSTNAME / --host HOSTNAME : BioMart server (default: #{@host})"
	puts "  -p PORT / --port PORT         : port to use (default: #{@port})"
	puts '  -v / --verify                 : verify results to contents of .reference files'
end

options = OptionParser.new { |option|
	option.on('-h', '--host HOSTNAME') { |hostname| @host = hostname }
	option.on('-p', '--port PORT') { |portnumber| @port = portnumber.to_i }
	option.on('-v', '--verify') { verify = true }
}

begin
	options.parse!
rescue OptionParser::InvalidOption
	print_help
	exit
end

if ARGV.length != 0 then
	print_help
	exit
end

server = "#{@host}"
server << ":#{@port}" unless @port == 80

def query(server, accesspoint, query)
	sparql = SPARQL::Client.new("http://#{server}/martsemantics/#{accesspoint}/SPARQLXML/get/")

	query = sparql.query(query)

	return query
end

Dir.glob('*.sparql').each { |filename|

	query = IO.read(filename)
	reference = IO.read(filename.sub(/\.sparql$/, '.reference')).split("\n") if verify

	filename.sub!(/\.sparql$/, '')

	result = nil
	begin
		result = query(server, filename, query)
	rescue
		puts '! ' << filename
	end

	unless result then
		puts '! ' << filename
		next
	end

	puts '. ' << filename

	variables = nil
	result.each { |solution|
		unless variables then
			variables = []
			solution.each_name { |name| variables << name.to_s }
			variables.sort!
		end
		row = ''
		variables.each { |name|
			row << solution[name].to_s
			
			row << "\t" unless name == variables.last
		}
		prefix = '  '
		if (verify) then
			prefix = '= ' 
			prefix = '? ' unless reference.shift == "  #{row}"
		end

		puts "#{prefix}#{row}"
	}

}

