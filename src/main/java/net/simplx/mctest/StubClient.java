package net.simplx.mctest;

import com.mojang.authlib.properties.PropertyMap;
import java.io.File;
import java.net.Proxy;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.RunArgs.Directories;
import net.minecraft.client.RunArgs.Game;
import net.minecraft.client.RunArgs.Network;
import net.minecraft.client.util.Session;
import net.minecraft.client.util.Session.AccountType;

public class StubClient extends MinecraftClient {

  public static final Directories STUB_DIRECTORIES = new Directories(new File("."), new File("."),
      new File("."), "foo");
  private static final Game STUB_GAME = new Game(true, "0.0", "s", true, true);
  private static final Network STUB_NETWORK = new Network(
      new Session("tester", "1-2-3-4-5", "dummy", Optional.empty(), Optional.empty(),
          AccountType.LEGACY), new PropertyMap(), new PropertyMap(), Proxy.NO_PROXY);

  public StubClient() {
    super(new RunArgs(STUB_NETWORK, null, STUB_DIRECTORIES, STUB_GAME, null));
  }
}
