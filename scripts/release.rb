#!/usr/bin/env ruby

require 'fileutils'
PREFIX = 'https://github.com/wvlet/airframe'
RELEASE_NOTES_FILE = "doc/docs/release-notes.md"

current_branch = `git rev-parse --abbrev-ref HEAD`.strip
abort("release.rb must run on master branch. The current branch is #{current_branch}") if current_branch != "master"

last_tag = `git describe --tags --abbrev=0`.chomp
last_version = last_tag.sub("v", "")
puts "last version: #{last_version}"

now = Time.new
(year, month, patch) = last_version.split('.').map{|x| x.to_i}

if year == now.year-2000 && month == now.month
  patch = patch + 1
else
  patch = 0
end
default_version = "#{now.year-2000}.#{now.month}.#{patch}"
print "next version (default: #{default_version})? "
next_version = STDIN.gets.chomp

next_version = default_version if next_version.empty?

logs = `git log #{last_tag}..HEAD --pretty=format:'%h %s'`
logs = logs.gsub(/\#([0-9]+)/, "[#\\1](#{PREFIX}/issues/\\1)")

new_release_notes = []
new_release_notes <<= "\#\# #{next_version}\n"
new_release_notes <<= logs.split(/\n/)
  .reject{|line| line.include?("#{last_version} release notes")}
  .map{|x|
    rev = x[0..6]
    "- #{x[8..-1]} [[#{rev}](#{PREFIX}/commit/#{rev})]\n"
  }

release_notes = []
notes = File.readlines(RELEASE_NOTES_FILE)

release_notes <<= notes[0..7]
release_notes <<= new_release_notes
release_notes <<= "\n"
release_notes <<= notes[8..-1]

TMP_RELEASE_NOTES_FILE = 'target/release_notes.md'
File.delete(TMP_RELEASE_NOTES_FILE) if File.exists?(TMP_RELEASE_NOTES_FILE)
File.write("#{TMP_RELEASE_NOTES_FILE}.tmp", release_notes.join)
system("cat #{TMP_RELEASE_NOTES_FILE}.tmp | vim - -c ':f #{TMP_RELEASE_NOTES_FILE}' -c ':9'")
File.delete("#{TMP_RELEASE_NOTES_FILE}.tmp")

abort("The release note file is not saved. Aborted") unless File.exists?(TMP_RELEASE_NOTES_FILE)

def run(cmd)
  puts cmd
  system cmd
end

# Tagging the commit first
run "git tag v#{next_version}"
run "git push --tags"
FileUtils.cp(TMP_RELEASE_NOTES_FILE, RELEASE_NOTES_FILE)
File.delete(TMP_RELEASE_NOTES_FILE)

run "git commit #{RELEASE_NOTES_FILE} -m \"[doc] Add #{next_version} release notes\""
run "git push"
