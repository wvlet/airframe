#!/usr/bin/env ruby

require 'fileutils'
PREFIX = 'https://github.com/wvlet/airframe'
RELEASE_NOTES_FILE = "docs/release-notes.md"

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

new_release_notes = []
new_release_notes <<= "\#\# #{next_version}\n\n"
new_release_notes <<= "[Release notes](https://github.com/wvlet/airframe/releases/tag/v#{next_version})\n"

system("mkdir -p target")
TMP_RELEASE_NOTES_FILE = "target/release_notes_#{next_version}.md"
File.delete(TMP_RELEASE_NOTES_FILE) if File.exists?(TMP_RELEASE_NOTES_FILE)
File.write("#{TMP_RELEASE_NOTES_FILE}.tmp", new_release_notes.join)
system("cat #{TMP_RELEASE_NOTES_FILE}.tmp | vim - -c ':f #{TMP_RELEASE_NOTES_FILE}' -c ':3'")
File.delete("#{TMP_RELEASE_NOTES_FILE}.tmp")

abort("The release note file is not saved. Aborted") unless File.exists?(TMP_RELEASE_NOTES_FILE)

def run(cmd)
  puts cmd
  system cmd
end

release_notes = []
notes = File.readlines(RELEASE_NOTES_FILE)
edited_new_release_notes = File.readlines(TMP_RELEASE_NOTES_FILE)

release_notes <<= notes[0..7]
release_notes <<= edited_new_release_notes
release_notes <<= "\n"
release_notes <<= notes[8..-1]

# Update the release note
File.write(RELEASE_NOTES_FILE, release_notes.join)

version_link = "https://wvlet.org/airframe/docs/release-notes\##{next_version.gsub(/\W/,'')}"

run "git commit #{RELEASE_NOTES_FILE} -m \"Release #{next_version}\""
run "git tag -a -m \"Airframe #{next_version}\" -m \"Release notes: #{version_link}\" v#{next_version}"
run "git push --follow-tags"

File.delete(TMP_RELEASE_NOTES_FILE)
