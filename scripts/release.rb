#!/usr/bin/env ruby

current_branch = `git rev-parse --abbrev-ref HEAD`.strip
abort("release.rb must run on master or main branch. The current branch is #{current_branch}") unless ["master", "main"].include?(current_branch)

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

run "git tag -a -m \"Airframe #{next_version}\" v#{next_version}"
run "git push --follow-tags"
