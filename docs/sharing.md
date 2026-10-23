To create a network share on Windows, you can use the built-in File Sharing properties to make any folder accessible to other devices on your local network.
Here is the step-by-step process to configure your network settings, share the folder, and access it from another computer.

Step 1: Enable Network Sharing Settings

Before sharing a specific folder, you must ensure your Windows system allows network sharing:
1. Open the Start Menu, type Control Panel, and open it.
2. Click on Network and Internet, then select Network and Sharing Center.
3. On the left sidebar, click Change advanced sharing settings.
4. Expand your current network profile (usually Private) and select Turn on network discovery and Turn on file and printer sharing.
5. Expand All Networks, ensure Password protected sharing is turned On (for security), and save your change


Step 2: Share the Folder

You can share an existing folder or create a new one.
1. Right-click the folder you want to share and select Properties.
2. Navigate to the Sharing tab at the top.
3. Click the Advanced Sharing... button.
4. Check the box that says Share this folder.
5. (Optional) Click Permissions to choose who can see it. By default, "Everyone" has Read access. You can change this to Full Control or restrict it to specific local user accounts.
6. Click Apply, then OK, and close the Properties window.
   Note: Alternatively, you can use the basic Share... button on the Sharing tab to quickly select specific people or the "Everyone" group from a dropdown lis


Step 3: Access the Share From Another PC

To open your newly shared folder from a different Windows computer on the same local network:

Step	Action	Example
1	Press Win + R on the second computer to open the Run dialog box.	—
2	Type two backslashes \\ followed by the Computer Name or IP Address of the host computer, then press Enter.	\\Desktop-PC or \\192.168.1.50
3	Enter the username and password of an account that exists on the host computer to gain access.	HostUser + Password

Once authenticated, the shared folder will appear, allowing you to drag and drop files depending on the permissions you granted.


To find your Computer Name and IP Address on Windows, you can use the Windows Settings app or a quick command prompt shortcut.
Here are the fastest ways to find both pieces of information.

1. How to Find Your Computer Name

Your computer name (also called device name or hostname) identifies your PC on the local network.
• The Quickest Way: Press Windows Key + Pause/Break on your keyboard to instantly open your system info page.
• Via Settings:
1. Open the Start Menu and click the Settings gear icon (or press Win + I).
2. Select System from the sidebar.
3. Scroll down and click About.
4. Look for Device name at the very top of the specifications list.




2. How to Find Your IP Address

Your local IP address (usually starting with 192.168. or 10.) tells other devices exactly where to route data on your home router.

Method A: Using Settings (Easiest)

1. Open Settings (Win + I).
2. Click on Network & internet on the left menu.
3. Click on Wi-Fi or Ethernet (depending on how your PC connects to the internet).
4. Click on your connected network name properties.
5. Scroll down to the bottom. Your address will be listed next to IPv4 address.

Method B: Using Command Prompt (Fastest)

1. Press Win + R to open the Run dialog box.
2. Type cmd and press Enter.
3. In the black window, type ipconfig and press Enter.
4. Look for the line that says IPv4 Address under your active connection (Wi-Fi or Ethernet). It will look something like 192.168.1.X.




Direct Comparison Overview


Information Type	What it Looks Like (Example)	Best Used For
Computer Name	DESKTOP-R47K9LM or Office-PC	Easy to remember; best for stable, permanent network shortcuts (\\Office-PC).
IP Address	192.168.1.45	Best if network name resolution fails; can change periodically unless made "static".

If you are having trouble connecting the two computers, let me know if you would like help troubleshooting a "Network Path Not Found" error, or if you want to learn how to make your IP address permanent (Static IP) so it never changes.