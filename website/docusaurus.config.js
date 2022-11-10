module.exports={
  "title": "Airframe",
  "tagline": "Essential Building Blocks For Scala",
  "url": "https://wvlet.org",
  "baseUrl": "/airframe/",
  "organizationName": "wvlet",
  "projectName": "airframe",
  "scripts": [
    "https://buttons.github.io/buttons.js"
  ],
  "favicon": "img/favicon.ico",
  "customFields": {
    "users": [
      {
        "caption": "Treasure Data",
        "image": "/img/undraw_open_source.svg",
        "infoLink": "https://www.treasuredata.com",
        "pinned": true
      }
    ],
    "blogSidebarTitle": {
      "default": "Recent posts",
      "all": "All blog posts"
    },
    "facebookAppId": "3112325918843547",
    "repoUrl": "https://github.com/wvlet/airframe"
  },
  "onBrokenLinks": "log",
  "onBrokenMarkdownLinks": "log",
  "presets": [
    [
      "@docusaurus/preset-classic",
      {
        "docs": {
          "path": "../docs",
          "showLastUpdateAuthor": true,
          "showLastUpdateTime": true,
            "sidebarPath": require.resolve("./sidebars.json")
        },
        "blog": {
          "path": "blog"
        },
        "theme": {
            "customCss": [require.resolve("./src/css/customTheme.css")]
        },
        "googleAnalytics": {
          "trackingID": "UA-98364158-1"
        }
      }
    ]
  ],
  "plugins": [],
  "themeConfig": {
    "navbar": {
      "title": "Airframe",
      "logo": {
        "src": "img/favicon.ico"
      },
      "items": [
        {
          "href": "/airframe/docs/",
          "label": "Docs",
          "position": "left"
        },
        {
          "href": "/airframe/docs/release-notes",
          "label": "Release Notes",
          "position": "left"
        },
        {
          "href": "https://github.com/wvlet/airframe/",
          "label": "GitHub",
          "position": "left"
        }
      ]
    },
    "image": "img/poster.png",
    "footer": {
      "links": [],
      "copyright": "Copyright © 2022 wvlet.org",
      "logo": {
        "src": "img/favicon.ico"
      }
    },
    "algolia": {
      "apiKey": "71b7e81be03c97dcd37b7a0efc8d6b76",
      "appId": "airframe",
      "indexName": "airframe"
    }
  }
}
