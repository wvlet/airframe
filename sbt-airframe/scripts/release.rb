#!/usr/bin/env ruby

require 'fileutils'
PREFIX = 'https://github.com/wvlet/sbt-airframe'
RELEASE_NOTES_FILE = "release-notes.md"
MAIN_BRANCH="main"

current_branch = `git rev-parse --abbrev-ref HEAD`.strip
abort("release.rb must run on main branch. The current branch is #{current_branch}") if current_branch != MAIN_BRANCH

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
new_release_notes <<= "\#\# #{next_version}\n\n"
new_release_notes <<= logs.split(/\n/)
  .reject{|line| line.include?("#{last_version} release notes")}
  .map{|x|
    m = x.match(/(^[0-9a-f]+)\s+(.*)/)
    rev = m[1]
    "- #{m[2]} [[#{rev}](#{PREFIX}/commit/#{rev})]\n"
  }

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

release_notes <<= notes[0..3]
release_notes <<= edited_new_release_notes
release_notes <<= "\n"
release_notes <<= notes[4..-1]

# Update the release note
File.write(RELEASE_NOTES_FILE, release_notes.join)

version_link = "https://github.com/wvlet/sbt-airframe/blob/main/#{RELEASE_NOTES_FILE}\##{next_version.gsub(/\W/,'')}"

run "git commit #{RELEASE_NOTES_FILE} -m \"Release #{next_version}\""
run "git tag -a -m \"sbt-airframe #{next_version}\" -m \"Release notes: #{version_link}\" v#{next_version}"
run "git push --follow-tags"

File.delete(TMP_RELEASE_NOTES_FILE)
